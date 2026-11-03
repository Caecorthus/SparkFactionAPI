package dev.caecorthus.sparkfactionapi.impl.mapreset;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.component.SparkFactionMapResetComponent;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-only lifecycle of the lobby map reset. Wathe copied the reset template onto the train in one tick right
 * after the start fade. Now a start ({@code GameFunctions.startGame}: the auto-start countdown reaching zero,
 * {@code /wathe:start} or the horn) first copies the map gradually while players stay in the lobby, then starts the
 * round with the same mode, effect and time. That round finds the map already reset ("prepared"), so Wathe's own
 * copy is narrowed to one block.
 * 大厅地图重置的仅服务端生命周期。Wathe 原本在开局淡入后于一个 tick 内把重置模板复制到车厢。现在开局
 * （{@code GameFunctions.startGame}：自动开局倒计时归零、{@code /wathe:start} 或汽笛）会先在玩家留在大厅时渐进复制地图，
 * 再以相同的模式、效果与时长开局。该局发现地图已重置（"已准备"），因此 Wathe 自己的复制被缩小为一个方块。
 *
 * <p>Only one reset runs at a time. While it runs, other starts and new votes are refused, the auto-start countdown
 * waits at zero, and map-changing administrator commands are refused. The lobby is untouched and the ready area is
 * counted when the round actually starts, so players can still board during the reset.
 * 同一时间只运行一个重置。重置期间其他开局与新投票被拒绝、自动开局倒计时停在零、更改地图的管理员命令被拒绝。
 * 大厅不受影响，且准备区人数在真正开局时才统计，因此重置期间玩家仍可登车。
 */
public final class LobbyMapReset {
    private static final String START_BLOCKED_KEY = "message.sparkfactionapi.map_reset.start_blocked";
    private static final String COMMAND_BLOCKED_KEY = "message.sparkfactionapi.map_reset.command_blocked";
    /**
     * FULL-level tickets (radius 0) load the chunks without ticking them, so blocks placed early cannot tick, flow or
     * fall before the rest of the copy arrives (unless players nearby already tick them, as at Wathe's round start).
     * FULL 级票据（半径 0）加载区块但不让其 tick，先放下的方块不会在其余部分复制完之前 tick、流动或坠落
     * （除非附近玩家已让它们 tick，与 Wathe 开局时相同）。
     */
    private static final ChunkTicketType<ChunkPos> TICKET =
            ChunkTicketType.create("sparkfactionapi_map_reset", Comparator.comparingLong(ChunkPos::toLong));

    private static @Nullable MapResetJob job;
    /** The start that triggered the running reset, replayed when it ends. / 触发当前重置的开局，重置结束时重放。 */
    private static @Nullable PendingStart pendingStart;
    /** True while that replayed start runs, so it is not turned into another reset. / 重放开局期间为 true，避免再次转为重置。 */
    private static boolean replayingStart;
    private static int completeDisplayTicks;
    /** Worlds whose map is reset and untouched since. / 地图已重置且之后未变动的世界。 */
    private static final Map<RegistryKey<World>, Prepared> PREPARED = new HashMap<>();
    private static @Nullable HeldTickets held;

    /**
     * The probe source block plus the template and paste offset the reset used. Wathe's copy is narrowed only while
     * both are unchanged, so an edit that bypasses commands (another mod, a datapack function) still gets a full copy.
     * 探针源方块以及重置时使用的模板与粘贴偏移。只有两者都未改变时才缩小 Wathe 的复制，
     * 因此绕过命令的修改（其他模组、数据包函数）仍会得到完整复制。
     */
    private record Prepared(BlockPos probe, Box template, Vec3i pasteOffset) {
    }

    private record HeldTickets(ServerWorld world, List<ChunkPos> chunks, int releaseTick) {
    }

    private record PendingStart(ServerWorld world, GameMode gameMode, MapEffect mapEffect, int time) {
    }

    private LobbyMapReset() {
    }

    /** True while a reset runs on this server. / 本服务器正在运行重置时为 true。 */
    public static boolean isResetting() {
        return job != null && !job.isFinished();
    }

    /**
     * Wathe {@code GameFunctions.startGame}, at HEAD. Returns true when Wathe must not start: either a reset is
     * already running (the start is refused with a notice), or this start was turned into a reset that will replay
     * it. Anything Wathe would refuse itself (a vote, too few ready players, a running round) is left to Wathe, so
     * its own notices still appear and no reset runs in vain.
     * 在 HEAD 处调用。返回 true 表示 Wathe 不应开局：要么已有重置在运行（拒绝并提示），要么本次开局已转为重置，
     * 结束后会重放。Wathe 自己会拒绝的情况（投票中、准备人数不足、对局进行中）交给 Wathe，保留其提示且不会白白重置。
     */
    public static boolean interceptStart(ServerWorld world, GameMode gameMode, MapEffect mapEffect, int time) {
        if (replayingStart) {
            return false;
        }
        if (isResetting()) {
            notifyStartRefused(world);
            return true;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        boolean resetFirst = LobbyMapResetRules.resetsBeforeStart(
                game.getGameStatus() == GameWorldComponent.GameStatus.INACTIVE,
                MapVotingComponent.KEY.get(world.getServer().getScoreboard()).isVotingActive(),
                isPrepared(world),
                GameFunctions.getReadyPlayerCount(world),
                gameMode.minPlayerCount
        );
        if (!resetFirst || !begin(world)) {
            return false;
        }
        pendingStart = new PendingStart(world, gameMode, mapEffect, time);
        return true;
    }

    /** Returns whether a reset started; false leaves the map to Wathe's full copy. / 返回是否开始了重置；false 时交给 Wathe 的完整复制。 */
    private static boolean begin(ServerWorld world) {
        MinecraftServer server = world.getServer();
        releaseHeldTickets();
        PREPARED.remove(world.getRegistryKey());
        completeDisplayTicks = 0;
        progress(server).setProgress(LobbyMapResetRules.INACTIVE);

        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(world);
        Box template = areas.getResetTemplateArea();
        if (template == null || areas.getResetPasteOffset() == null) {
            return false;
        }
        // Same corner arithmetic as Wathe's tryResetTrain. / 与 Wathe tryResetTrain 相同的角点计算。
        BlockPos sourceMin = BlockPos.ofFloored(template.getMinPos());
        BlockPos sourceMax = BlockPos.ofFloored(template.getMaxPos());
        BlockBox source = BlockBox.create(sourceMin, sourceMax);
        BlockPos targetMin = BlockPos.ofFloored(template.offset(Vec3d.of(areas.getResetPasteOffset())).getMinPos());
        BlockPos offset = new BlockPos(
                targetMin.getX() - source.getMinX(),
                targetMin.getY() - source.getMinY(),
                targetMin.getZ() - source.getMinZ()
        );
        List<ChunkPos> chunks = chunksAround(source, offset);
        for (ChunkPos chunk : chunks) {
            world.getChunkManager().addTicket(TICKET, chunk, 0, chunk);
        }
        job = new MapResetJob(world, source, offset, chunks, template, areas.getResetPasteOffset());
        progress(server).setProgress(0);
        SparkFactionApiMod.LOGGER.info(
                "Lobby map reset started in {}: {} template blocks, {} chunks.",
                world.getRegistryKey().getValue(), job.volume(), chunks.size()
        );
        return true;
    }

    private static boolean isPrepared(ServerWorld world) {
        Prepared prepared = PREPARED.get(world.getRegistryKey());
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(world);
        return prepared != null
                && prepared.template().equals(areas.getResetTemplateArea())
                && prepared.pasteOffset().equals(areas.getResetPasteOffset());
    }

    /** Template and paste boxes, plus one chunk of margin so edge blocks have loaded neighbors. / 模板与粘贴区域外加一圈区块，保证边缘方块的邻居已加载。 */
    private static List<ChunkPos> chunksAround(BlockBox source, BlockPos offset) {
        List<ChunkPos> chunks = new ArrayList<>();
        addChunks(chunks, source.getMinX(), source.getMinZ(), source.getMaxX(), source.getMaxZ());
        addChunks(
                chunks,
                source.getMinX() + offset.getX(),
                source.getMinZ() + offset.getZ(),
                source.getMaxX() + offset.getX(),
                source.getMaxZ() + offset.getZ()
        );
        return chunks;
    }

    private static void addChunks(List<ChunkPos> chunks, int minX, int minZ, int maxX, int maxZ) {
        for (int x = (minX >> 4) - 1; x <= (maxX >> 4) + 1; x++) {
            for (int z = (minZ >> 4) - 1; z <= (maxZ >> 4) + 1; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (!chunks.contains(chunk)) {
                    chunks.add(chunk);
                }
            }
        }
    }

    /** Server tick: advances the copy and the "complete" display. / 服务端 tick：推进复制与"完成"显示。 */
    static void tick(MinecraftServer server) {
        if (held != null && server.getTicks() >= held.releaseTick()) {
            releaseHeldTickets();
        }
        SparkFactionMapResetComponent progress = progress(server);
        if (job == null) {
            if (completeDisplayTicks > 0 && --completeDisplayTicks == 0) {
                progress.setProgress(LobbyMapResetRules.INACTIVE);
            }
            return;
        }
        MapResetJob current = job;
        if (GameWorldComponent.KEY.get(current.world()).getGameStatus() != GameWorldComponent.GameStatus.INACTIVE) {
            // Something started a round anyway; Wathe's own full copy at round start repairs the map.
            // 有东西仍然开了局；Wathe 开局时自己的完整复制会修复地图。
            current.fail("a round started during the reset");
            pendingStart = null;
        } else {
            try {
                current.tick();
            } catch (RuntimeException exception) {
                SparkFactionApiMod.LOGGER.error("Lobby map reset failed; Wathe will copy the map at round start.", exception);
                current.fail(exception.toString());
            }
        }
        if (!current.isFinished()) {
            progress.setProgress(current.percent());
            return;
        }
        job = null;
        if (current.phase() == MapResetJob.Phase.DONE) {
            if (current.probe() != null) {
                PREPARED.put(
                        current.world().getRegistryKey(),
                        new Prepared(current.probe(), current.template(), current.pasteOffset())
                );
            }
            held = new HeldTickets(
                    current.world(),
                    current.chunks(),
                    server.getTicks() + LobbyMapResetRules.TICKET_IDLE_RELEASE_TICKS
            );
            progress.setProgress(LobbyMapResetRules.COMPLETE);
            completeDisplayTicks = LobbyMapResetRules.COMPLETE_DISPLAY_TICKS;
            SparkFactionApiMod.LOGGER.info(
                    "Lobby map reset finished in {}: {} blocks changed.",
                    current.world().getRegistryKey().getValue(), current.placed()
            );
        } else {
            releaseTickets(current.world(), current.chunks());
            progress.setProgress(LobbyMapResetRules.INACTIVE);
            SparkFactionApiMod.LOGGER.warn(
                    "Lobby map reset in {} stopped ({}); Wathe will copy the map at round start instead.",
                    current.world().getRegistryKey().getValue(), current.failure()
            );
        }
        replayPendingStart(current.world());
    }

    /**
     * Starts the round the reset was waiting for, through Wathe's own {@code startGame}, so it counts the ready area
     * now and shows its usual notices. After a failed reset the round still starts and Wathe copies the whole map.
     * 通过 Wathe 自己的 {@code startGame} 开始重置所等待的对局，从而按此刻的准备区计数并显示其常规提示。
     * 重置失败时仍会开局，由 Wathe 复制整张地图。
     */
    private static void replayPendingStart(ServerWorld world) {
        PendingStart start = pendingStart;
        pendingStart = null;
        if (start == null || start.world() != world
                || GameWorldComponent.KEY.get(world).getGameStatus() != GameWorldComponent.GameStatus.INACTIVE) {
            return;
        }
        replayingStart = true;
        try {
            GameFunctions.startGame(world, start.gameMode(), start.mapEffect(), start.time());
        } finally {
            replayingStart = false;
        }
    }

    /** The same action-bar notice Wathe uses for an active vote. / 与 Wathe 投票中相同的动作栏提示。 */
    private static void notifyStartRefused(ServerWorld world) {
        Text message = Text.translatableWithFallback(
                START_BLOCKED_KEY,
                "The map is resetting (%s%%). The game starts as soon as it finishes.",
                job.percent()
        ).formatted(Formatting.YELLOW);
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(message, true);
        }
    }

    /** Wathe {@code MapVotingComponent.startVoting}: refuses a new vote while a reset runs. / 重置期间拒绝新的投票。 */
    public static boolean refuseVote() {
        if (!isResetting()) {
            return false;
        }
        SparkFactionApiMod.LOGGER.warn("Map voting refused: the lobby map reset is still running.");
        return true;
    }

    /**
     * {@code CommandManager.execute}: refuses map-changing Wathe commands while a reset runs. Outside a reset, a
     * map-variables write makes every prepared map stale, since the template or areas may have changed.
     * 重置期间拒绝更改地图的 Wathe 命令。重置之外写入地图变量会让所有已准备的地图失效，因为模板或区域可能已改变。
     */
    public static boolean refuseCommand(ParseResults<ServerCommandSource> parseResults) {
        List<String> literals = literals(parseResults);
        if (isResetting() && LobbyMapResetRules.blocksCommand(literals)) {
            parseResults.getContext().getSource().sendError(Text.translatableWithFallback(
                    COMMAND_BLOCKED_KEY,
                    "The map is resetting (%s%%). Map commands are unavailable until it finishes.",
                    job.percent()
            ));
            return true;
        }
        if (LobbyMapResetRules.editsMapVariables(literals)) {
            PREPARED.clear();
        }
        return false;
    }

    /** Literals of the parsed command, following redirects such as {@code execute ... run}. / 已解析命令的字面量，跟随 {@code execute ... run} 等重定向。 */
    private static List<String> literals(ParseResults<ServerCommandSource> parseResults) {
        List<String> literals = new ArrayList<>();
        for (CommandContextBuilder<ServerCommandSource> context = parseResults.getContext();
             context != null;
             context = context.getChild()) {
            for (ParsedCommandNode<ServerCommandSource> node : context.getNodes()) {
                if (node.getNode() instanceof LiteralCommandNode<ServerCommandSource> literal) {
                    literals.add(literal.getLiteral());
                }
            }
        }
        return literals;
    }

    /**
     * Wathe {@code tryResetTrain}, at its template-area reads: for a prepared world, narrows the copy to the probe
     * block. The copy still runs, changes that block (barrier, then the template state) and reports success, so its
     * retry handling, entity discards and add-ons hooked on its result (SparkStrength sabotage) behave as before.
     * 对已准备的世界，把复制缩小为探针方块。复制照常运行，改变该方块（先屏障再恢复模板状态）并报告成功，
     * 因此其重试处理、实体清除以及依赖其结果的附属模组（SparkStrength 破坏系统）行为不变。
     */
    public static Box templateForWatheCopy(ServerWorld world, MapVariablesWorldComponent areas, Box template) {
        Prepared prepared = PREPARED.get(world.getRegistryKey());
        if (prepared == null
                || !prepared.template().equals(template)
                || !prepared.pasteOffset().equals(areas.getResetPasteOffset())) {
            return template;
        }
        Vec3d corner = Vec3d.of(prepared.probe());
        return new Box(corner, corner);
    }

    /**
     * Wathe {@code tryResetTrain} returned: it consumes the prepared state, and after a success (false, "no retry")
     * the train chunks are released shortly after the round started.
     * 消耗已准备状态；复制成功（返回 false，"无需重试"）时还会在开局后不久释放车厢区块。
     */
    public static void onWatheCopyFinished(ServerWorld world, boolean retry) {
        // A retry also drops the prepared state: the next attempt is Wathe's full copy, so a probe that stopped
        // matching the template can never make the round wait forever.
        // 重试同样丢弃已准备状态：下一次尝试即为 Wathe 的完整复制，与模板不再匹配的探针永远不会让对局无限等待。
        PREPARED.remove(world.getRegistryKey());
        if (retry) {
            return;
        }
        if (held != null && held.world() == world) {
            held = new HeldTickets(
                    held.world(),
                    held.chunks(),
                    world.getServer().getTicks() + LobbyMapResetRules.TICKET_HOLD_AFTER_START_TICKS
            );
        }
    }

    /** Server stopped: a reset never survives a restart. / 服务器停止：重置不会跨越重启。 */
    static void clear() {
        job = null;
        pendingStart = null;
        replayingStart = false;
        held = null;
        completeDisplayTicks = 0;
        PREPARED.clear();
    }

    private static void releaseHeldTickets() {
        if (held != null) {
            releaseTickets(held.world(), held.chunks());
            held = null;
        }
    }

    private static void releaseTickets(ServerWorld world, List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            world.getChunkManager().removeTicket(TICKET, chunk, 0, chunk);
        }
    }

    private static SparkFactionMapResetComponent progress(MinecraftServer server) {
        return SparkFactionMapResetComponent.KEY.get(server.getScoreboard());
    }
}
