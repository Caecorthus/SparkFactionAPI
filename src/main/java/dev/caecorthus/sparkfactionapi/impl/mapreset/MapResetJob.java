package dev.caecorthus.sparkfactionapi.impl.mapreset;

import dev.doctor4t.wathe.entity.FirecrackerEntity;
import dev.doctor4t.wathe.entity.NoteEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.index.WatheEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One gradual copy of a world's reset template onto its play area. It performs the same passes as Wathe's
 * {@code GameFunctions.tryResetTrain}, in the same order and with the same block flags, but spends at most
 * {@link LobbyMapResetRules#TICK_BUDGET_NANOS} per tick: snapshot the template (full cubes, then block entities, then
 * the rest), barrier the target in reverse order, place every block, update neighbors, copy scheduled ticks, and
 * discard bodies, items, firecrackers and notes.
 * 一次将世界重置模板渐进复制到游玩区域的过程。它与 Wathe {@code GameFunctions.tryResetTrain} 执行相同的各遍处理，
 * 顺序和方块标志也相同，但每 tick 最多占用 {@link LobbyMapResetRules#TICK_BUDGET_NANOS}：快照模板（先完整方块，再方块实体，
 * 再其余方块）、倒序把目标设为屏障、放置所有方块、更新邻居、复制计划刻，最后清除尸体、掉落物、鞭炮与纸条。
 *
 * <p>Unlike Wathe, a block entity's data is loaded right after its block is placed, in the same tick. Wathe loads it
 * after all blocks are placed, which is the same tick there; spread over ticks, a ticking hopper or furnace could
 * otherwise run between the two steps.
 * 与 Wathe 不同，方块实体数据在放置其方块后立即于同一 tick 内载入。Wathe 在所有方块放置完后才载入，在那里仍是同一 tick；
 * 分散到多个 tick 后，否则漏斗或熔炉等会在两步之间运行。
 */
final class MapResetJob {
    enum Phase {
        LOADING,
        SNAPSHOT,
        CLEAR,
        PLACE,
        BLOCK_ENTITY_STATES,
        NEIGHBORS,
        DONE,
        FAILED
    }

    private static final int PROBE_ANY = 1;
    private static final int PROBE_AIR = 2;
    private static final int PROBE_ISOLATED_AIR = 3;

    private record Entry(BlockPos target, BlockState state, @Nullable NbtCompound nbt,
                         @Nullable ComponentMap components) {
    }

    private final ServerWorld world;
    private final BlockBox source;
    private final BlockPos offset;
    private final List<ChunkPos> chunks;
    private final Box template;
    private final Vec3i pasteOffset;
    private final int sizeX;
    private final int sizeY;
    private final int volume;

    private final List<Entry> fullCubes = new ArrayList<>();
    private final List<Entry> blockEntities = new ArrayList<>();
    private final List<Entry> others = new ArrayList<>();
    private List<Entry> order = List.of();
    private int blockEntityStart;
    private int blockEntityEnd;

    private Phase phase = Phase.LOADING;
    private int loadingTicks;
    private int cursor;
    private int placed;
    private @Nullable BlockPos probe;
    private int probeRank;
    private @Nullable String failure;

    MapResetJob(ServerWorld world, BlockBox source, BlockPos offset, List<ChunkPos> chunks, Box template,
                Vec3i pasteOffset) {
        this.world = world;
        this.source = source;
        this.offset = offset;
        this.chunks = List.copyOf(chunks);
        this.template = template;
        this.pasteOffset = pasteOffset;
        this.sizeX = source.getBlockCountX();
        this.sizeY = source.getBlockCountY();
        this.volume = sizeX * sizeY * source.getBlockCountZ();
    }

    ServerWorld world() {
        return world;
    }

    List<ChunkPos> chunks() {
        return chunks;
    }

    Box template() {
        return template;
    }

    Vec3i pasteOffset() {
        return pasteOffset;
    }

    Phase phase() {
        return phase;
    }

    int volume() {
        return volume;
    }

    int placed() {
        return placed;
    }

    @Nullable String failure() {
        return failure;
    }

    /**
     * A template block that is not a barrier, as a source position. Wathe's start-of-round copy is narrowed to this
     * one block so it still runs, counts one changed block and reports success. Null when every block is a barrier.
     * 一个不是屏障的模板方块（源坐标）。Wathe 开局时的复制会被缩小到这一个方块，使其照常运行、计入一个改变的方块并报告成功。
     * 模板全是屏障时为 null。
     */
    @Nullable BlockPos probe() {
        return probe;
    }

    int percent() {
        return switch (phase) {
            case LOADING -> 0;
            case SNAPSHOT -> LobbyMapResetRules.percent(0, cursor, volume);
            case CLEAR -> LobbyMapResetRules.percent(1, cursor, volume);
            case PLACE -> LobbyMapResetRules.percent(2, cursor, volume);
            case BLOCK_ENTITY_STATES -> LobbyMapResetRules.percent(3, 0, volume);
            case NEIGHBORS -> LobbyMapResetRules.percent(3, cursor, volume);
            case DONE -> LobbyMapResetRules.COMPLETE;
            case FAILED -> LobbyMapResetRules.INACTIVE;
        };
    }

    boolean isFinished() {
        return phase == Phase.DONE || phase == Phase.FAILED;
    }

    void fail(String reason) {
        if (!isFinished()) {
            this.failure = reason;
            this.phase = Phase.FAILED;
        }
    }

    /** Runs one tick's share of the copy. / 执行本 tick 分到的复制工作。 */
    void tick() {
        if (isFinished()) {
            return;
        }
        if (phase == Phase.LOADING) {
            tickLoading();
            return;
        }
        long deadline = System.nanoTime() + LobbyMapResetRules.TICK_BUDGET_NANOS;
        int sinceClockCheck = 0;
        while (!isFinished()) {
            step();
            if (++sinceClockCheck >= LobbyMapResetRules.CLOCK_CHECK_INTERVAL) {
                sinceClockCheck = 0;
                if (System.nanoTime() >= deadline) {
                    return;
                }
            }
        }
    }

    private void tickLoading() {
        for (ChunkPos chunk : chunks) {
            if (world.getChunkManager().getWorldChunk(chunk.x, chunk.z) == null) {
                if (++loadingTicks > LobbyMapResetRules.CHUNK_LOAD_TIMEOUT_TICKS) {
                    fail("template or play area chunks did not load");
                }
                return;
            }
        }
        phase = Phase.SNAPSHOT;
        cursor = 0;
    }

    private void step() {
        switch (phase) {
            case SNAPSHOT -> snapshot();
            case CLEAR -> clear();
            case PLACE -> place();
            case BLOCK_ENTITY_STATES -> blockEntityStates();
            case NEIGHBORS -> neighbors();
            default -> {
            }
        }
    }

    private void snapshot() {
        if (cursor >= volume) {
            order = new ArrayList<>(fullCubes.size() + blockEntities.size() + others.size());
            order.addAll(fullCubes);
            blockEntityStart = order.size();
            order.addAll(blockEntities);
            blockEntityEnd = order.size();
            order.addAll(others);
            fullCubes.clear();
            blockEntities.clear();
            others.clear();
            phase = Phase.CLEAR;
            cursor = 0;
            return;
        }
        // Same z, y, x nesting as Wathe so blocks of one category keep its order.
        // 与 Wathe 相同的 z、y、x 嵌套顺序，保证同类方块保持其顺序。
        int index = cursor++;
        BlockPos pos = new BlockPos(
                source.getMinX() + index % sizeX,
                source.getMinY() + (index / sizeX) % sizeY,
                source.getMinZ() + index / (sizeX * sizeY)
        );
        BlockState state = world.getBlockState(pos);
        BlockPos target = pos.add(offset);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntities.add(new Entry(
                    target,
                    state,
                    blockEntity.createComponentlessNbt(world.getRegistryManager()),
                    blockEntity.getComponents()
            ));
            return;
        }
        if (probeRank < PROBE_ISOLATED_AIR && !state.isOf(Blocks.BARRIER)) {
            considerProbe(pos, state);
        }
        if (!state.isOpaqueFullCube(world, pos) && !state.isFullCube(world, pos)) {
            others.add(new Entry(target, state, null, null));
        } else {
            fullCubes.add(new Entry(target, state, null, null));
        }
    }

    /**
     * Prefers air inside the template whose six neighbors are air too: flipping it to a barrier and back then sends no
     * shape update to anything real, so no flower, torch or door half can pop off around it.
     * 优先选择模板内部、六个邻居也都是空气的空气方块：把它变成屏障再变回来时，不会向任何实际方块发送形状更新，
     * 周围的花、火把或门都不会掉落。
     */
    private void considerProbe(BlockPos pos, BlockState state) {
        int rank = PROBE_ANY;
        if (state.isAir()) {
            rank = isInterior(pos) && neighborsAreAir(pos) ? PROBE_ISOLATED_AIR : PROBE_AIR;
        }
        if (rank > probeRank) {
            probe = pos;
            probeRank = rank;
        }
    }

    private boolean isInterior(BlockPos pos) {
        return pos.getX() > source.getMinX() && pos.getX() < source.getMaxX()
                && pos.getY() > source.getMinY() && pos.getY() < source.getMaxY()
                && pos.getZ() > source.getMinZ() && pos.getZ() < source.getMaxZ();
    }

    private boolean neighborsAreAir(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!world.getBlockState(pos.offset(direction)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private void clear() {
        if (cursor >= order.size()) {
            phase = Phase.PLACE;
            cursor = 0;
            return;
        }
        Entry entry = order.get(order.size() - 1 - cursor++);
        Clearable.clear(world.getBlockEntity(entry.target()));
        world.setBlockState(entry.target(), Blocks.BARRIER.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private void place() {
        if (cursor >= order.size()) {
            phase = Phase.BLOCK_ENTITY_STATES;
            cursor = blockEntityStart;
            return;
        }
        Entry entry = order.get(cursor++);
        if (world.setBlockState(entry.target(), entry.state(), Block.NOTIFY_LISTENERS)) {
            placed++;
        }
        if (entry.nbt() == null) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(entry.target());
        if (blockEntity != null) {
            blockEntity.readComponentlessNbt(entry.nbt(), world.getRegistryManager());
            blockEntity.setComponents(entry.components());
            blockEntity.markDirty();
        }
    }

    /**
     * Wathe sets every block-entity state once more after all blocks are placed, which restores a hand-set state
     * (connections, attachment, waterlogging) that a later neighbor's shape update rewrote.
     * Wathe 在所有方块放置完后再次设置每个方块实体的状态，恢复被之后邻居形状更新改写的手动状态（连接、附着、含水）。
     */
    private void blockEntityStates() {
        if (cursor >= blockEntityEnd) {
            phase = Phase.NEIGHBORS;
            cursor = 0;
            return;
        }
        Entry entry = order.get(cursor++);
        world.setBlockState(entry.target(), entry.state(), Block.NOTIFY_LISTENERS);
    }

    private void neighbors() {
        if (cursor >= order.size()) {
            finish();
            return;
        }
        Entry entry = order.get(order.size() - 1 - cursor++);
        world.updateNeighbors(entry.target(), entry.state().getBlock());
    }

    private void finish() {
        order = List.of();
        if (placed == 0) {
            // Wathe treats "nothing copied" as a failed attempt too. / Wathe 同样把"未复制任何方块"视为失败。
            fail("no blocks were copied");
            return;
        }
        world.getBlockTickScheduler().scheduleTicks(world.getBlockTickScheduler(), source, offset);
        for (PlayerBodyEntity body : world.getEntitiesByType(WatheEntities.PLAYER_BODY, entity -> true)) {
            body.discard();
        }
        for (ItemEntity item : world.getEntitiesByType(EntityType.ITEM, entity -> true)) {
            item.discard();
        }
        for (FirecrackerEntity firecracker : world.getEntitiesByType(WatheEntities.FIRECRACKER, entity -> true)) {
            firecracker.discard();
        }
        for (NoteEntity note : world.getEntitiesByType(WatheEntities.NOTE, entity -> true)) {
            note.discard();
        }
        phase = Phase.DONE;
    }
}
