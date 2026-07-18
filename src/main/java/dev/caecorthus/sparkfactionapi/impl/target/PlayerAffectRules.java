package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.PlayerAffectPolicy;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Aggregates player-affect policies with order-independent deny-wins semantics.
 * 以与顺序无关的“拒绝优先”语义汇总玩家影响策略。
 */
public final class PlayerAffectRules {
    private PlayerAffectRules() {
    }

    public static boolean canAffectPlayer(
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    ) {
        return canAffectPlayer(
                PlayerAffectPolicies.policies(),
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
