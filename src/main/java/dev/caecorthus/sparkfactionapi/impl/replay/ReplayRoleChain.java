package dev.caecorthus.sparkfactionapi.impl.replay;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure role-chain queries over {@link ReplayRoleTimeline} used by the replay presentation.
 * 回放展示使用的纯身份链查询，基于 {@link ReplayRoleTimeline}。
 */
public final class ReplayRoleChain {
    private ReplayRoleChain() {
    }

    /** One chain role; {@code reachedAtTick} is null for the starting role. 身份链中的一项；起始身份的 {@code reachedAtTick} 为 null。 */
    public record Step(Identifier role, @Nullable Long reachedAtTick) {
    }

    /**
     * The player's roles in order, each role reached by a change carrying that change's world tick.
     * 按顺序排列的玩家身份；由变化得到的身份附带该变化的世界刻。
     */
    public static List<Step> steps(ReplayRoleTimeline timeline, UUID player) {
        List<Identifier> history = timeline.history(player);
        List<ReplayRoleTimeline.Change> changes = timeline.changes(player);
        int leading = history.size() - changes.size();
        List<Step> steps = new ArrayList<>(history.size());
        for (int i = 0; i < leading; i++) {
            steps.add(new Step(history.get(i), null));
        }
        for (ReplayRoleTimeline.Change change : changes) {
            steps.add(new Step(change.to(), change.worldTick()));
        }
        return List.copyOf(steps);
    }

    /**
     * Role to show for a line: the role held just before event {@code seq}, the earliest known role when the player
     * had none yet, or the final role when no event is being formatted.
     * 某行应显示的身份：事件 {@code seq} 前一刻的身份；尚无身份时取最早已知身份；未在格式化事件时取最终身份。
     */
    public static @Nullable Identifier roleForEvent(ReplayRoleTimeline timeline, UUID player, @Nullable Integer seq) {
        if (seq == null) {
            return timeline.finalRole(player);
        }
        Identifier role = timeline.roleAt(player, seq);
        if (role != null) {
            return role;
        }
        List<Identifier> history = timeline.history(player);
        return history.isEmpty() ? timeline.finalRole(player) : history.getFirst();
    }

    /**
     * Role history for {@code ReplayPlayerView}; never empty, falling back to the final role and then {@code noRole}.
     * 供 {@code ReplayPlayerView} 使用的身份历史；不为空，依次回退为最终身份与 {@code noRole}。
     */
    public static List<Identifier> nonEmptyHistory(ReplayRoleTimeline timeline, UUID player, Identifier noRole) {
        List<Identifier> history = timeline.history(player);
        if (!history.isEmpty()) {
            return history;
        }
        Identifier finalRole = timeline.finalRole(player);
        return List.of(finalRole != null ? finalRole : noRole);
    }
}
