package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the shared player-affect veto to every player-attributed Wathe death.
 * 对所有可归因到玩家的 Wathe 死亡应用统一玩家影响否决。
 */
@Mixin(value = GameFunctions.class, remap = false)
public abstract class GameFunctionsPlayerAffectMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sparkfactionapi$guardPlayerAttributedDeath(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force,
            CallbackInfo ci
    ) {
        if (killer != null && !PlayerAffectMixinGuard.allows(killer, victim, deathReason)) {
            ci.cancel();
        }
    }
}
