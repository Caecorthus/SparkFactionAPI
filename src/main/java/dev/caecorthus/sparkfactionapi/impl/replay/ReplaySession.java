package dev.caecorthus.sparkfactionapi.impl.replay;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayBadge;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.caecorthus.sparkfactionapi.impl.text.FactionRoundEndTextRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private final Header header;
    private final ReplayRoleTimeline timeline;
    private final NameSources nameSources;
    private final Map<UUID, String> names;
    private final Map<UUID, Text> statuses;
    private final Map<UUID, Text> tooltips;
    private final Map<UUID, ReplayGenerator.PlayerInfo> playerInfo;
    private final Map<UUID, ScreenFacts> screenFacts;
    private final Map<UUID, String> outsiderNames = new HashMap<>();
    private final List<FormattedLine> formattedLines = new ArrayList<>();
    private @Nullable List<Text> generatedLines;
    private @Nullable List<Text> summaryLines;
    private boolean renderFailureLogged;

    private ReplaySession(
            Header header,
            ReplayRoleTimeline timeline,
            NameSources nameSources,
            Map<UUID, String> names,
            Map<UUID, Text> statuses,
            Map<UUID, Text> tooltips,
            Map<UUID, ReplayGenerator.PlayerInfo> playerInfo,
            Map<UUID, ScreenFacts> screenFacts
    ) {
        this.header = header;
        this.timeline = timeline;
        this.nameSources = nameSources;
        this.names = names;
        this.statuses = statuses;
        this.tooltips = tooltips;
        this.playerInfo = playerInfo;
        this.screenFacts = screenFacts;
    }

    /**
     * Match-level facts for the screen header; {@code endTick} is Wathe's {@code match_end} tick (same tick as
     * generation) or the world time when it is missing.
     * 界面头部的对局级信息；{@code endTick} 取 Wathe {@code match_end} 的刻（与生成同刻），缺失时取世界时间。
     */
    record Header(
            UUID matchId,
            Identifier gameMode,
            Identifier mapEffect,
            long startTick,
            long endTick,
            Optional<Text> outcome
    ) {
    }

    /** Per-participant facts frozen for the screen only. 仅供界面使用的单名参与者冻结信息。 */
    record ScreenFacts(ReplayPlayerStatusRules.Status status, Text faction, List<ReplayBadge> badges) {
        ScreenFacts {
            badges = List.copyOf(badges);
        }
    }

    /** A line exactly as Wathe formatted it, before the time prefix. Wathe 格式化出的单行（不含时间前缀）。 */
    record FormattedLine(GameRecordEvent event, Text text) {
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
        Map<UUID, ScreenFacts> screenFacts = new HashMap<>();
        var contributors = ReplayTooltipContributors.entries();
        var badgeContributors = ReplayBadgeContributors.entries();
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
            Text faction = ReplayTooltipBuilder.faction(finalFaction(online, game, timeline.finalRole(uuid)));
            tooltips.put(uuid, ReplayTooltipBuilder.tooltip(
                    name,
                    ReplayTooltipBuilder.roleChain(ReplayRoleChain.steps(timeline, uuid), startTick, true),
                    faction,
                    statusText,
                    ReplayTooltipBuilder.contributorLines(view, contributors)
            ));
            screenFacts.put(uuid, new ScreenFacts(
                    status,
                    faction,
                    ReplaySnapshotBuilder.contributorBadges(view, badgeContributors)
            ));
            playerInfo.put(uuid, watheInfo(name, history.getFirst()));
        }
        Header header = new Header(
                match.getMatchId(),
                match.getGameModeId(),
                match.getMapEffectId(),
                startTick,
                endTick(events, world.getTime()),
                outcome(world, game, roundEnd, roundEndIsCurrent, nameSources)
        );
        return new ReplaySession(
                header,
                timeline,
                nameSources,
                names,
                statuses,
                tooltips,
                playerInfo,
                screenFacts
        );
    }

    public boolean isFor(GameRecordManager.MatchRecord match) {
        return header.matchId().equals(match.getMatchId());
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
                    .append(ReplayTooltipBuilder.roleChain(ReplayRoleChain.steps(timeline, uuid), header.startTick(), false))
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

    /** Called once per non-null formatter result, in Wathe's order. 每个非 null 的格式化结果按 Wathe 顺序调用一次。 */
    void recordFormattedLine(GameRecordEvent event, Text text) {
        formattedLines.add(new FormattedLine(event, text));
    }

    List<FormattedLine> formattedLines() {
        return Collections.unmodifiableList(formattedLines);
    }

    /** Built once and shared by every summary recipient. 只构建一次，所有摘要接收者共用。 */
    List<Text> summaryLines() {
        if (summaryLines == null) {
            List<ReplayPlayerStatusRules.Status> participantStatuses = new ArrayList<>(screenFacts.size());
            screenFacts.values().forEach(facts -> participantStatuses.add(facts.status()));
            summaryLines = ReplaySummary.lines(ReplaySummary.stats(
                    timeline,
                    participantStatuses,
                    header.startTick(),
                    header.endTick()
            ));
        }
        return summaryLines;
    }

    Header header() {
        return header;
    }

    /** Participants in roster order. 按名单顺序排列的参与者。 */
    Set<UUID> participants() {
        return Collections.unmodifiableSet(names.keySet());
    }

    String name(UUID uuid) {
        return names.get(uuid);
    }

    List<ReplayRoleChain.Step> roleSteps(UUID uuid) {
        return ReplayRoleChain.steps(timeline, uuid);
    }

    List<Identifier> nonEmptyHistory(UUID uuid) {
        return ReplayRoleChain.nonEmptyHistory(timeline, uuid, WatheRoles.NO_ROLE.identifier());
    }

    @Nullable ScreenFacts screenFacts(UUID uuid) {
        return screenFacts.get(uuid);
    }

    @Nullable Text tooltip(UUID uuid) {
        return tooltips.get(uuid);
    }

    /**
     * Frozen tooltip instance to owner, compared by identity: every name span this session rendered references
     * exactly these instances through its SHOW_TEXT hover.
     * 冻结提示实例到所属玩家的映射，按同一引用比较：本会话渲染的每个名字片段都通过 SHOW_TEXT 悬停引用这些实例。
     */
    Map<Text, UUID> tooltipOwners() {
        Map<Text, UUID> owners = new IdentityHashMap<>();
        tooltips.forEach((uuid, tooltip) -> owners.put(tooltip, uuid));
        return owners;
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

    /** Last {@code match_end} tick, else {@code fallback}. 最后一个 {@code match_end} 的刻，否则为 {@code fallback}。 */
    private static long endTick(List<GameRecordEvent> events, long fallback) {
        long end = fallback;
        int seq = Integer.MIN_VALUE;
        for (GameRecordEvent event : events) {
            if (GameRecordTypes.MATCH_END.equals(event.type()) && event.seq() > seq) {
                seq = event.seq();
                end = event.worldTick();
            }
        }
        return end;
    }

    /**
     * Screen-only result title; any failure yields no title instead of breaking the chat replay presentation.
     * 仅供界面使用的结果标题；任何失败都只是不显示标题，不影响聊天回放展示。
     */
    private static Optional<Text> outcome(
            ServerWorld world,
            GameWorldComponent game,
            GameRoundEndComponent roundEnd,
            boolean roundEndIsCurrent,
            NameSources nameSources
    ) {
        try {
            GameFunctions.WinStatus status = roundEnd.getWinStatus();
            Identifier firstWinnerRole = null;
            for (GameRoundEndComponent.RoundEndData row : roundEnd.getPlayers()) {
                if (row.isWinner()) {
                    firstWinnerRole = row.role();
                    break;
                }
            }
            UUID looseEndWinner = game.getLooseEndWinner();
            return ReplayOutcomeRules.outcome(
                    roundEndIsCurrent && roundEnd.getRoundGameMode() != WatheGameModes.DISCOVERY,
                    status,
                    status == GameFunctions.WinStatus.NEUTRAL ? customWinTitle(world) : null,
                    firstWinnerRole,
                    ReplayNameRenderer::roleColor,
                    looseEndWinner == null ? "" : nameSources.resolve(looseEndWinner, true)
            );
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to resolve the replay result title; the screen shows none", e);
            return Optional.empty();
        }
    }

    /** Same source as FactionAPI's RoundTextRendererMixin. 与 FactionAPI 的 RoundTextRendererMixin 取值一致。 */
    private static @Nullable Text customWinTitle(ServerWorld world) {
        SparkFactionRoundEndComponent customRoundEnd = SparkFactionRoundEndComponent.KEY.get(world.getScoreboard());
        Identifier winningFaction = customRoundEnd.getWinningFaction();
        if (winningFaction == null || !customRoundEnd.hasCustomWin()) {
            return null;
        }
        return SparkFactionApi.getFaction(winningFaction).map(FactionRoundEndTextRules::winTitle).orElse(null);
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
