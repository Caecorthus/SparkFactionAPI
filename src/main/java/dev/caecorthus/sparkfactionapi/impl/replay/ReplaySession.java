package dev.caecorthus.sparkfactionapi.impl.replay;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Per-generation replay state, built once from the finished match while Wathe has not yet reset players or roles.
 * Names, statuses and hover tooltips are frozen here so every line of one replay renders consistently.
 * 单次回放生成的状态；在 Wathe 重置玩家与身份之前由已结束的对局一次性构建。名字、状态与悬停提示在此冻结，
 * 保证同一回放中每一行渲染一致。
 */
public final class ReplaySession {
    private static final String WATHE_UNKNOWN_ROLE_KEY = "unknown";

    private final UUID matchId;
    private final long startTick;
    private final ReplayRoleTimeline timeline;
    private final NameSources nameSources;
    private final Map<UUID, String> names;
    private final Map<UUID, Text> statuses;
    private final Map<UUID, Text> tooltips;
    private final Map<UUID, ReplayGenerator.PlayerInfo> playerInfo;
    private final Map<UUID, String> outsiderNames = new HashMap<>();
    private @Nullable List<Text> generatedLines;
    private boolean renderFailureLogged;

    private ReplaySession(
            UUID matchId,
            long startTick,
            ReplayRoleTimeline timeline,
            NameSources nameSources,
            Map<UUID, String> names,
            Map<UUID, Text> statuses,
            Map<UUID, Text> tooltips,
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfo
    ) {
        this.matchId = matchId;
        this.startTick = startTick;
        this.timeline = timeline;
        this.nameSources = nameSources;
        this.names = names;
        this.statuses = statuses;
        this.tooltips = tooltips;
        this.playerInfo = playerInfo;
    }

    public static ReplaySession build(ServerWorld world, GameRecordManager.MatchRecord match) {
        List<GameRecordEvent> events = match.getEvents();
        long startTick = match.getStartTick();
        MinecraftServer server = world.getServer();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        ReplayRoleTimeline timeline = ReplayRoleTimeline.fromEvents(events);
        NameSources nameSources = new NameSources(assignedNames(events), game.getGameProfiles(), server);

        Map<UUID, ReplayPlayerStatusRules.PlayerFacts> facts = ReplayPlayerStatusRules.digest(events);
        GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(world.getScoreboard());
        Map<UUID, Identifier> roundEndRoles = new HashMap<>();
        Map<UUID, Boolean> roundEndWinners = new HashMap<>();
        for (GameRoundEndComponent.RoundEndData row : roundEnd.getPlayers()) {
            roundEndRoles.put(row.player().getId(), row.role());
            roundEndWinners.put(row.player().getId(), row.isWinner());
        }
        boolean roundEndIsCurrent = ReplayPlayerStatusRules.roundEndIsCurrent(
                roundEnd.getWinStatus() == GameFunctions.WinStatus.NONE,
                roundEndRoles,
                liveRoundEndRoles(game)
        );

        Map<UUID, String> names = new LinkedHashMap<>();
        Map<UUID, Text> statuses = new HashMap<>();
        Map<UUID, Text> tooltips = new HashMap<>();
        Map<UUID, ReplayGenerator.PlayerInfo> playerInfo = new HashMap<>();
        var contributors = ReplayTooltipContributors.entries();
        for (UUID uuid : timeline.players()) {
            String name = nameSources.resolve(uuid, true);
            names.put(uuid, name);
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
            List<Identifier> history = ReplayRoleChain.nonEmptyHistory(timeline, uuid, WatheRoles.NO_ROLE.identifier());

            ReplayPlayerStatusRules.Status status = ReplayPlayerStatusRules.decide(
                    facts.getOrDefault(uuid, ReplayPlayerStatusRules.PlayerFacts.NONE),
                    online != null,
                    game.isPlayerDead(uuid),
                    ReplayPlayerStatusRules.result(roundEndIsCurrent, roundEndWinners.get(uuid))
            );
            Text statusText = ReplayTooltipBuilder.status(status, startTick);
            statuses.put(uuid, statusText);

            ReplayParticipantView view = new ReplayParticipantView(uuid, name, world, online, history);
            tooltips.put(uuid, ReplayTooltipBuilder.tooltip(
                    name,
                    ReplayTooltipBuilder.roleChain(ReplayRoleChain.steps(timeline, uuid), startTick, true),
                    ReplayTooltipBuilder.faction(finalFaction(online, game, timeline.finalRole(uuid))),
                    statusText,
                    ReplayTooltipBuilder.contributorLines(view, contributors)
            ));
            playerInfo.put(uuid, watheInfo(name, history.getFirst()));
        }
        return new ReplaySession(
                match.getMatchId(),
                startTick,
                timeline,
                nameSources,
                names,
                statuses,
                tooltips,
                playerInfo
        );
    }

    public boolean isFor(GameRecordManager.MatchRecord match) {
        return matchId.equals(match.getMatchId());
    }

    /**
     * Fresh mutable copy with real {@code PlayerInfo} values (Wathe returns a new map per call, so callers may mutate).
     * 带真实 {@code PlayerInfo} 值的新可变副本（Wathe 每次调用都返回新 map，调用方可能修改它）。
     */
    public Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCopy() {
        return new HashMap<>(playerInfo);
    }

    public Text nameWithRole(UUID uuid, @Nullable Integer seq) {
        String name = names.get(uuid);
        if (name == null) {
            return Text.literal(outsiderName(uuid));
        }
        return ReplayNameRenderer.renderNameWithRole(
                name,
                ReplayRoleChain.roleForEvent(timeline, uuid, seq),
                tooltips.get(uuid)
        );
    }

    public Text plainName(UUID uuid) {
        String name = names.get(uuid);
        if (name == null) {
            return Text.literal(outsiderName(uuid));
        }
        return ReplayNameRenderer.renderPlainName(name, timeline.finalRole(uuid), tooltips.get(uuid));
    }

    /** Gray title, one line per participant in timeline order, then a spacer. 灰色标题、按时间线顺序每人一行，末尾空行。 */
    public List<Text> rosterLines() {
        List<Text> lines = new ArrayList<>(names.size() + 2);
        lines.add(Text.translatable("replay.sparkfactionapi.roster.title").formatted(Formatting.GRAY));
        for (UUID uuid : names.keySet()) {
            lines.add(Text.literal("  ")
                    .append(plainName(uuid))
                    .append(Text.literal("  "))
                    .append(ReplayTooltipBuilder.roleChain(ReplayRoleChain.steps(timeline, uuid), startTick, false))
                    .append(Text.literal("  · ").formatted(Formatting.GRAY))
                    .append(statuses.get(uuid)));
        }
        lines.add(Text.empty());
        return lines;
    }

    public void recordGeneratedLines(List<Text> lines) {
        this.generatedLines = List.copyOf(lines);
    }

    public @Nullable List<Text> generatedLines() {
        return generatedLines;
    }

    /** True only once, so a render bug logs once per replay. 仅首次返回 true，渲染错误每次回放只记录一次。 */
    boolean markRenderFailureLogged() {
        if (renderFailureLogged) {
            return false;
        }
        renderFailureLogged = true;
        return true;
    }

    private String outsiderName(UUID uuid) {
        return outsiderNames.computeIfAbsent(uuid, ignored -> nameSources.resolve(uuid, false));
    }

    /**
     * First non-blank name from {@code sources}, evaluated lazily in order, else the 8-character UUID prefix Wathe uses.
     * 按顺序惰性取第一个非空名字；都没有时使用 Wathe 同款的 8 位 UUID 前缀。
     */
    @SafeVarargs
    static String resolveName(UUID uuid, Supplier<String>... sources) {
        for (Supplier<String> source : sources) {
            String name = source.get();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return uuid.toString().substring(0, 8);
    }

    private static Map<UUID, String> assignedNames(List<GameRecordEvent> events) {
        Map<UUID, String> names = new HashMap<>();
        for (GameRecordEvent event : events) {
            if (!GameRecordTypes.ROLE_ASSIGNED.equals(event.type())) {
                continue;
            }
            NbtCompound player = event.data().getCompound("player");
            if (player.containsUuid("uuid")) {
                names.putIfAbsent(player.getUuid("uuid"), player.getString("name"));
            }
        }
        return names;
    }

    /** Role map exactly as Wathe/FactionAPI turn it into round-end rows. 与 Wathe/FactionAPI 生成结算行时相同口径的身份表。 */
    private static Map<UUID, Identifier> liveRoundEndRoles(GameWorldComponent game) {
        Map<UUID, GameProfile> profiles = game.getGameProfiles();
        Map<UUID, Identifier> roles = new HashMap<>();
        game.getRoles().forEach((uuid, role) -> {
            if (role != null && role != WatheRoles.NO_ROLE && profiles.get(uuid) != null) {
                roles.put(uuid, role.identifier());
            }
        });
        return roles;
    }

    private static Identifier finalFaction(
            @Nullable ServerPlayerEntity online,
            GameWorldComponent game,
            @Nullable Identifier finalRoleId
    ) {
        if (online != null) {
            try {
                return SparkFactionApi.resolveEffectiveFaction(online, game);
            } catch (RuntimeException e) {
                SparkFactionApiMod.LOGGER.warn(
                        "Effective faction lookup failed for replay player {}; using the base faction",
                        online.getUuid(),
                        e
                );
            }
        }
        Role role = finalRoleId == null ? null : WatheRoles.getRole(finalRoleId);
        return role == null ? FactionIds.NONE : SparkFactionApi.resolveBaseFaction(role);
    }

    /** Mirrors Wathe buildPlayerInfoCache values for one role. 与 Wathe buildPlayerInfoCache 生成的值保持一致。 */
    private static ReplayGenerator.PlayerInfo watheInfo(String name, Identifier roleId) {
        Role role = WatheRoles.getRole(roleId);
        return new ReplayGenerator.PlayerInfo(
                name,
                role != null ? "announcement.role." + roleId.getPath() : WATHE_UNKNOWN_ROLE_KEY,
                role != null ? role.color() : ReplayNameRenderer.UNKNOWN_ROLE_COLOR
        );
    }

    /**
     * Name fallback chain: role_assigned snapshot (participants only), game profiles, online player, user cache.
     * 名字回退链：开局身份快照（仅参与者）、对局档案、在线玩家、用户缓存。
     */
    private record NameSources(Map<UUID, String> assigned, Map<UUID, GameProfile> profiles, MinecraftServer server) {
        String resolve(UUID uuid, boolean participant) {
            return resolveName(
                    uuid,
                    () -> participant ? assigned.get(uuid) : null,
                    () -> {
                        GameProfile profile = profiles.get(uuid);
                        return profile == null ? null : profile.getName();
                    },
                    () -> {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                        return player == null ? null : player.getGameProfile().getName();
                    },
                    () -> {
                        UserCache cache = server.getUserCache();
                        return cache == null ? null : cache.getByUuid(uuid).map(GameProfile::getName).orElse(null);
                    }
            );
        }
    }
}
