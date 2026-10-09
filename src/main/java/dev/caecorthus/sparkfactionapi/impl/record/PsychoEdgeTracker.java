package dev.caecorthus.sparkfactionapi.impl.record;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Turns Wathe's psycho START/END events into real edges for one match. Wathe's {@code stopPsycho} fires END
 * unconditionally ({@code reset()} at game start, in the lobby, and Noelle's Jester reset after death all fire END for
 * players who are not in psycho), and {@code startPsycho} fires START again while already in psycho (refresh). An END
 * counts only for a player whose START was counted in the same match; a START for a player already counted is a
 * refresh. A new match id forgets everything.
 * 把 Wathe 的疯魔 START/END 事件转换为单局内的真实边沿。Wathe 的 {@code stopPsycho} 无条件触发 END（开局与大厅中的
 * {@code reset()}、Noelle 小丑死亡后的重置都会给未处于疯魔的玩家触发 END），{@code startPsycho} 在已处于疯魔时也会
 * 再次触发 START（刷新）。仅当同一局内已计入该玩家的 START 时 END 才计入；对已计入玩家的 START 视为刷新。对局标识
 * 变化时清空全部状态。
 */
public final class PsychoEdgeTracker {
    private @Nullable UUID matchId;
    private final Set<UUID> inPsycho = new HashSet<>();

    /** True when this START is a new entry, false for a refresh. START 为新进入时返回 true，刷新时返回 false。 */
    public synchronized boolean start(UUID match, UUID player) {
        enter(match);
        return inPsycho.add(Objects.requireNonNull(player, "player"));
    }

    /** True when this END closes a counted START. END 结束一次已计入的 START 时返回 true。 */
    public synchronized boolean end(UUID match, UUID player) {
        enter(match);
        return inPsycho.remove(Objects.requireNonNull(player, "player"));
    }

    private void enter(UUID match) {
        Objects.requireNonNull(match, "match");
        if (!match.equals(matchId)) {
            matchId = match;
            inPsycho.clear();
        }
    }
}
