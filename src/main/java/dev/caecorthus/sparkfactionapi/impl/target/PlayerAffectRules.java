package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.PlayerAffectPolicy;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Aggregates player-affect policies with order-independent deny-wins semantics.
 * 以与顺序无关的“拒绝优先”语义汇总玩家影响策略。
 */
public final class PlayerAffectRules {
    // Owns player-affect policy registration without assigning precedence. Copy-on-write, so a check iterates a
    // snapshot and a policy registered mid-check neither throws nor joins that check.
    // 持有玩家影响策略注册，但不赋予任何策略优先级。写时复制，检查遍历快照，检查中途注册的策略既不抛异常也不加入本次检查。
    private static final List<PlayerAffectPolicy> POLICIES = new CopyOnWriteArrayList<>();

    private PlayerAffectRules() {
    }

    public static void register(PlayerAffectPolicy policy) {
        POLICIES.add(Objects.requireNonNull(policy, "policy"));
    }

    public static boolean canAffectPlayer(
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    ) {
        return canAffectPlayer(
                POLICIES,
                actor,
                target,
                actionId,
                gameComponent
        );
    }

    public static boolean canAffectPlayer(
            Iterable<? extends PlayerAffectPolicy> policies,
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    ) {
        boolean allowed = true;
        for (PlayerAffectPolicy policy : policies) {
            if (!policy.canAffect(actor, target, actionId, gameComponent)) {
                allowed = false;
            }
        }
        return allowed;
    }
}
