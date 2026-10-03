package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player role history of one match, rebuilt from Wathe record events.
 * Wathe only snapshots roles at round start; this timeline adds {@code role_changed} events so a replay
 * line can show the role a player held when that line's event happened.
 * 单局内每名玩家的身份历史，由 Wathe 记录事件重建。Wathe 只在开局快照身份；本时间线叠加
 * {@code role_changed} 事件，使回放每一行都显示事件发生当时的身份。
 */
public final class ReplayRoleTimeline {
    /** Role-changed event NBT keys; stable replay contract. role_changed 事件的 NBT 键，属于稳定回放契约。 */
    public static final String KEY_PLAYER = "player";
    public static final String KEY_FROM = "from";
    public static final String KEY_TO = "to";
    public static final String KEY_CAUSE = "cause";
    public static final String KEY_SOURCE = "source";

    private final Map<UUID, Identifier> openingRoles;
    private final Map<UUID, List<Change>> changes;

    private ReplayRoleTimeline(Map<UUID, Identifier> openingRoles, Map<UUID, List<Change>> changes) {
        this.openingRoles = openingRoles;
        this.changes = changes;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads Wathe {@code role_assigned} snapshots and {@code sparkfactionapi:role_changed} events in sequence order.
     * 按序号读取 Wathe {@code role_assigned} 快照与 {@code sparkfactionapi:role_changed} 事件。
     */
    public static ReplayRoleTimeline fromEvents(List<GameRecordEvent> events) {
        List<GameRecordEvent> ordered = new ArrayList<>(events);
        ordered.sort((a, b) -> Integer.compare(a.seq(), b.seq()));
        Builder builder = builder();
        for (GameRecordEvent event : ordered) {
            NbtCompound data = event.data();
            if (GameRecordTypes.ROLE_ASSIGNED.equals(event.type())) {
                NbtCompound player = data.getCompound("player");
                Identifier role = readId(player, "role");
                if (player.containsUuid("uuid") && role != null) {
                    builder.opening(player.getUuid("uuid"), role);
                }
            } else if (SparkReplayApi.ROLE_CHANGED_EVENT_TYPE.equals(event.type())) {
                Identifier from = readId(data, KEY_FROM);
                Identifier to = readId(data, KEY_TO);
                if (!data.containsUuid(KEY_PLAYER) || to == null) {
                    continue;
                }
                Identifier cause = readId(data, KEY_CAUSE);
                UUID source = data.containsUuid(KEY_SOURCE) ? data.getUuid(KEY_SOURCE) : null;
                builder.change(data.getUuid(KEY_PLAYER), event.seq(), event.worldTick(), from, to, cause, source);
            }
        }
        return builder.build();
    }

    /**
     * Missing or empty strings read as null; {@code Identifier.tryParse("")} would yield {@code minecraft:}.
     * 缺失或空字符串读作 null；{@code Identifier.tryParse("")} 会得到 {@code minecraft:}。
     */
    private static @Nullable Identifier readId(NbtCompound data, String key) {
        if (!data.contains(key, NbtElement.STRING_TYPE)) {
            return null;
        }
        String raw = data.getString(key);
        return raw.isEmpty() ? null : Identifier.tryParse(raw);
    }

    /** Players in first-appearance order. 按首次出现顺序排列的玩家。 */
    public Set<UUID> players() {
        Set<UUID> players = new LinkedHashSet<>(openingRoles.keySet());
        players.addAll(changes.keySet());
        return Collections.unmodifiableSet(players);
    }

    /**
     * Role held immediately before the event with sequence {@code seq}; a change recorded at {@code seq}
     * itself is not yet applied, so a role-changed line shows its own "from" role.
     * 序号为 {@code seq} 的事件发生前一刻的身份；同序号的变化尚未生效，因此转化行本身显示变化前身份。
     */
    public @Nullable Identifier roleAt(UUID player, int seq) {
        Identifier role = openingRoles.get(player);
        for (Change change : changes.getOrDefault(player, List.of())) {
            if (change.seq() >= seq) {
                break;
            }
            role = change.to();
        }
        return role;
    }

    public @Nullable Identifier finalRole(UUID player) {
        List<Change> playerChanges = changes.getOrDefault(player, List.of());
        return playerChanges.isEmpty() ? openingRoles.get(player) : playerChanges.getLast().to();
    }

    /** Opening role followed by each distinct new role. 开局身份及之后每次变化的新身份。 */
    public List<Identifier> history(UUID player) {
        List<Identifier> history = new ArrayList<>();
        Identifier opening = openingRoles.get(player);
        if (opening != null) {
            history.add(opening);
        }
        for (Change change : changes.getOrDefault(player, List.of())) {
            if (history.isEmpty() && change.from() != null) {
                history.add(change.from());
            }
            history.add(change.to());
        }
        return List.copyOf(history);
    }

    public List<Change> changes(UUID player) {
        return changes.getOrDefault(player, List.of());
    }

    public record Change(
            int seq,
            long worldTick,
            @Nullable Identifier from,
            Identifier to,
            @Nullable Identifier cause,
            @Nullable UUID sourceUuid
    ) {
    }

    public static final class Builder {
        private final Map<UUID, Identifier> openingRoles = new LinkedHashMap<>();
        private final Map<UUID, List<Change>> changes = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder opening(UUID player, Identifier role) {
            openingRoles.putIfAbsent(player, role);
            return this;
        }

        public Builder change(
                UUID player,
                int seq,
                long worldTick,
                @Nullable Identifier from,
                Identifier to,
                @Nullable Identifier cause,
                @Nullable UUID sourceUuid
        ) {
            List<Change> playerChanges = changes.computeIfAbsent(player, ignored -> new ArrayList<>());
            playerChanges.add(new Change(seq, worldTick, from, to, cause, sourceUuid));
            playerChanges.sort((a, b) -> Integer.compare(a.seq(), b.seq()));
            return this;
        }

        public ReplayRoleTimeline build() {
            Map<UUID, List<Change>> frozen = new LinkedHashMap<>();
            changes.forEach((player, playerChanges) -> frozen.put(player, List.copyOf(playerChanges)));
            return new ReplayRoleTimeline(
                    Collections.unmodifiableMap(new LinkedHashMap<>(openingRoles)),
                    Collections.unmodifiableMap(frozen)
            );
        }
    }
}
