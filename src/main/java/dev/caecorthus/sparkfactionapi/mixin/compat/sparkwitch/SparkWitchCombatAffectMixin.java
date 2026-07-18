package dev.caecorthus.sparkfactionapi.mixin.compat.sparkwitch;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards Fire Poker and Ceremonial Sword effects before their custom attack paths run.
 * 在火钳与仪式剑的自定义攻击路径运行前拦截其效果。
 */
@Pseudo
@Mixin(targets = {
        "dev.caecorthus.sparkwitch.item.firepoker.FirePokerCombatService",
        "dev.caecorthus.sparkwitch.item.ceremonialsword.CeremonialSwordCombatService"
}, remap = false)
public abstract class SparkWitchCombatAffectMixin {
    @Inject(method = "tryHandleAttack", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardCustomMelee(
            Entity attacker,
            World world,
            Hand hand,
            Entity target,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (attacker instanceof ServerPlayerEntity actor
                && target instanceof ServerPlayerEntity playerTarget
                && !PlayerAffectMixinGuard.allows(
                        actor,
                        playerTarget,
                        Identifier.of("sparkwitch", "custom_melee")
                )) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "killWithCeremonialSword", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardCeremonialDashKill(
            ServerPlayerEntity actor,
            ServerPlayerEntity target,
            CallbackInfo ci
    ) {
        if (!PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("sparkwitch", "ceremonial_blade")
        )) {
            ci.cancel();
        }
    }
}
