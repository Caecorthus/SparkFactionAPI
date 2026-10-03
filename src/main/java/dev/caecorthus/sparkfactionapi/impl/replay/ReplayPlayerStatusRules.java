package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure rules for the end-of-round status shown in replay tooltips and the roster.
 * 回放悬停提示与名单中局末状态的纯规则。
 */
public final class ReplayPlayerStatusRules {
    /** Wathe join/leave player and death killer key. Wathe 加入/离开事件的玩家与死亡事件的凶手所在的键。 */
    static final String KEY_ACTOR = "actor";
    /** Wathe {@code GameRecordManager.recordDeath} writes the victim under this key. 死亡事件中受害者所在的键。 */
    static final String KEY_TARGET = "target";

    private ReplayPlayerStatusRules() {
    }

    public enum Presence {
        ALIVE,
        DEAD,
        LEFT
    }

    public enum Result {
        WIN,
        LOSE
    }

    /**
     * Facts about one player digested from the match record.
     * 从对局记录中提取的单名玩家事实。
     */
    public record PlayerFacts(boolean lastPresenceWasLeave, @Nullable Long lastDeathTick) {
        public static final PlayerFacts NONE = new PlayerFacts(false, null);
    }

    public record Status(Presence presence, @Nullable Long deathTick, @Nullable Result result) {
    }

    /**
     * Scans join/leave and death events in sequence order once for every player.
     * 按序号一次性扫描所有玩家的加入/离开与死亡事件。
     */
    public static Map<UUID, PlayerFacts> digest(List<GameRecordEvent> events) {
        List<GameRecordEvent> ordered = new ArrayList<>(events);
        ordered.sort((a, b) -> Integer.compare(a.seq(), b.seq()));
        Map<UUID, Boolean> lastPresenceWasLeave = new HashMap<>();
        Map<UUID, Long> lastDeathTick = new HashMap<>();
        for (GameRecordEvent event : ordered) {
            NbtCompound data = event.data();
            String type = event.type();
            if (GameRecordTypes.PLAYER_JOIN.equals(type) || GameRecordTypes.PLAYER_LEAVE.equals(type)) {
                if (data.containsUuid(KEY_ACTOR)) {
                    lastPresenceWasLeave.put(data.getUuid(KEY_ACTOR), GameRecordTypes.PLAYER_LEAVE.equals(type));
                }
            } else if (GameRecordTypes.DEATH.equals(type) && data.containsUuid(KEY_TARGET)) {
                lastDeathTick.put(data.getUuid(KEY_TARGET), event.worldTick());
            }
        }
        Map<UUID, PlayerFacts> facts = new HashMap<>();
        lastPresenceWasLeave.forEach((uuid, left) -> facts.put(uuid, new PlayerFacts(left, lastDeathTick.get(uuid))));
        lastDeathTick.forEach((uuid, tick) -> facts.putIfAbsent(uuid, new PlayerFacts(false, tick)));
        return facts;
    }

    /**
     * "Left" only when the last recorded presence change is a leave and the player is still offline; otherwise
     * "dead" when Wathe marks the player dead; otherwise "alive".
     * 仅当最后一次在场变化是离开且玩家仍离线时为“离开”；否则 Wathe 标记死亡时为“死亡”；否则为“存活”。
     */
    public static Status decide(PlayerFacts facts, boolean online, boolean dead, @Nullable Result result) {
        if (facts.lastPresenceWasLeave() && !online) {
            return new Status(Presence.LEFT, null, result);
        }
        if (dead) {
            return new Status(Presence.DEAD, facts.lastDeathTick(), result);
        }
        return new Status(Presence.ALIVE, null, result);
    }

    /**
     * Wathe never resets {@code GameRoundEndComponent} at round start, so a forced stop leaves the previous round's
     * rows in place. Rows are trusted only when a win was determined and they describe exactly the live role map
     * (Wathe and FactionAPI both build rows from that map, skipping no-role players and players without a profile).
     * Wathe 不会在开局重置 {@code GameRoundEndComponent}，强制结束时其中仍是上一局的结算行。仅当已判定胜负且结算行
     * 与当前身份表完全一致时才信任它（Wathe 与 FactionAPI 都由该表生成结算行，并跳过无身份及无档案的玩家）。
     */
    public static boolean roundEndIsCurrent(
            boolean winStatusIsNone,
            Map<UUID, Identifier> roundEndRoles,
            Map<UUID, Identifier> liveRoles
    ) {
        return !winStatusIsNone && !roundEndRoles.isEmpty() && roundEndRoles.equals(liveRoles);
    }

    /**
     * Win/lose only for a current round-end that lists the player; otherwise no result is shown.
     * 仅当结算数据属于本局且包含该玩家时给出胜负；否则不显示结果。
     */
    public static @Nullable Result result(boolean roundEndIsCurrent, @Nullable Boolean listedWinner) {
        if (!roundEndIsCurrent || listedWinner == null) {
            return null;
        }
        return listedWinner ? Result.WIN : Result.LOSE;
    }
}
