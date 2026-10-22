package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure extraction of the players a replay line involves, used by the screen's per-player filter.
 * Wathe {@code GameRecordManager#addEvent} writes the actor and target UUIDs under {@code actor}/{@code target};
 * {@code role_changed} uses {@code player}/{@code source} instead. Names the line actually renders (tagged while
 * rewriting) are appended so add-on keys such as a platter poisoner are covered too.
 * 纯规则：提取一条回放行涉及的玩家，供界面按玩家筛选。Wathe {@code GameRecordManager#addEvent} 以
 * {@code actor}/{@code target} 存储行为者与目标 UUID；{@code role_changed} 使用 {@code player}/{@code source}。
 * 行内实际渲染的名字（改写时被标记）也会追加进来，从而覆盖餐盘下毒者等附属模组自定义键。
 */
public final class ReplayInvolvedPlayers {
    private ReplayInvolvedPlayers() {
    }

    /** Event-data UUIDs first, then {@code mentioned}; deduplicated, order kept, capped. 先事件数据，再行内提及；去重、保序、截断。 */
    public static List<UUID> of(String eventType, NbtCompound data, Collection<UUID> mentioned) {
        Set<UUID> players = new LinkedHashSet<>();
        addUuid(players, data, ReplayPlayerStatusRules.KEY_ACTOR);
        addUuid(players, data, ReplayPlayerStatusRules.KEY_TARGET);
        if (SparkReplayApi.ROLE_CHANGED_EVENT_TYPE.equals(eventType)) {
            addUuid(players, data, ReplayRoleTimeline.KEY_PLAYER);
            addUuid(players, data, ReplayRoleTimeline.KEY_SOURCE);
        }
        for (UUID uuid : mentioned) {
            if (uuid != null) {
                players.add(uuid);
            }
        }
        List<UUID> ordered = new ArrayList<>(players);
        int max = ReplaySnapshot.MAX_LINE_PLAYERS;
        return List.copyOf(ordered.size() > max ? ordered.subList(0, max) : ordered);
    }

    private static void addUuid(Set<UUID> players, NbtCompound data, String key) {
        if (data.containsUuid(key)) {
            players.add(data.getUuid(key));
        }
    }
}
