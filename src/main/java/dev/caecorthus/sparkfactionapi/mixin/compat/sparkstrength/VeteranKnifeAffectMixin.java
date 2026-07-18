package dev.caecorthus.sparkfactionapi.mixin.compat.sparkstrength;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards Strength's Veteran knife before a stab charge is consumed.
 * 在消耗刺击次数前拦截 Strength 的老兵刀。
 */
@Pseudo
@Mixin(targets = "annina.sparkstrength.role.veteran.VeteranKnifeService", remap = false)
public abstract class VeteranKnifeAffectMixin {
    @Inject(method = "handleKnifeStab", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardVeteranKnife(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayerEntity actor = context.player();
        ServerPlayerEntity target = PlayerAffectMixinGuard.entityTarget(
                actor,
                PlayerAffectMixinGuard.intAccessor(payload, "target")
        );
        if (target != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                GameConstants.DeathReasons.KNIFE
        )) {
            cir.setReturnValue(true);
        }
    }
}
