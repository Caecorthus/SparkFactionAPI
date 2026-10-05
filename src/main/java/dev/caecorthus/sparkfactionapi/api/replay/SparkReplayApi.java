package dev.caecorthus.sparkfactionapi.api.replay;

import dev.caecorthus.sparkfactionapi.impl.replay.ReplayBadgeContributors;
import dev.caecorthus.sparkfactionapi.impl.replay.ReplayTooltipContributors;
import dev.caecorthus.sparkfactionapi.impl.replay.RoleChangeCauseScope;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Public seam for the end-of-round Wathe replay: role-change attribution and player hover tooltips.
 * Wathe 局末回放的公开接缝：身份转化归因与玩家名悬停提示。
 */
public final class SparkReplayApi {
    /**
     * Wathe record event type written for every mid-round role change; stable replay contract.
     * 对局中每次身份变化写入的 Wathe 记录事件类型；属于稳定回放契约。
     */
    public static final String ROLE_CHANGED_EVENT_TYPE = "sparkfactionapi:role_changed";

    private SparkReplayApi() {
    }

    /**
     * Runs {@code action}; any mid-round role change it performs is recorded with {@code cause} and {@code source}.
     * Scopes nest (innermost wins) and only tag changes on the current thread. The client translation key is
     * {@code replay.sparkfactionapi.role_changed.cause.<ns>.<path>} with a {@code .by} suffix when a source is set.
     * 执行 {@code action}；其中发生的对局内身份变化会附带原因与来源玩家写入回放。作用域可嵌套（内层优先），
     * 仅影响当前线程。客户端翻译键为 {@code replay.sparkfactionapi.role_changed.cause.<ns>.<path>}，有来源时追加 {@code .by}。
     */
    public static void withRoleChangeCause(Identifier cause, @Nullable PlayerEntity source, Runnable action) {
        withRoleChangeCause(cause, source == null ? null : source.getUuid(), action);
    }

    /**
     * UUID variant of {@link #withRoleChangeCause(Identifier, PlayerEntity, Runnable)} for offline sources.
     * 适用于离线来源玩家的 UUID 版本。
     */
    public static void withRoleChangeCause(Identifier cause, @Nullable UUID sourceUuid, Runnable action) {
        RoleChangeCauseScope.run(
                Objects.requireNonNull(cause, "cause"),
                sourceUuid,
                Objects.requireNonNull(action, "action")
        );
    }

    /**
     * Adds lines to every participant's replay hover tooltip, in registration order. Contributors run on the server
     * thread once per participant while the replay is generated, before Wathe resets players, so round-scoped state
     * is still readable. Re-registering an id replaces the earlier contributor in place.
     * 为每名参与者的回放悬停提示追加内容，按注册顺序执行。贡献者在生成回放时于服务端线程对每名参与者调用一次，
     * 早于 Wathe 重置玩家，因此仍可读取本局状态。重复注册同一 id 会原位替换旧贡献者。
     */
    public static void registerPlayerTooltipContributor(Identifier id, ReplayTooltipContributor contributor) {
        ReplayTooltipContributors.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(contributor, "contributor")
        );
    }

    /**
     * Adds badges (small coloured labels, e.g. traits) to every participant's card in the replay screen, in
     * registration order. Same lifecycle as tooltip contributors; re-registering an id replaces it in place.
     * 为回放界面中每名参与者的卡片追加标签（彩色小标签，例如词条），按注册顺序执行。生命周期与悬停提示贡献者相同；
     * 重复注册同一 id 会原位替换。
     */
    public static void registerPlayerBadgeContributor(Identifier id, ReplayBadgeContributor contributor) {
        ReplayBadgeContributors.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(contributor, "contributor")
        );
    }
}
