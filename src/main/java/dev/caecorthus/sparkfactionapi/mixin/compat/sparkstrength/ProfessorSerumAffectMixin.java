package dev.caecorthus.sparkfactionapi.mixin.compat.sparkstrength;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Guards local and remote Professor feeds before serum inventory is consumed.
 * 在消耗血清物品前拦截教授的近程与远程喂药。
 */
@Pseudo
@Mixin(targets = "annina.sparkstrength.role.professor.ProfessorSerumService", remap = false)
public abstract class ProfessorSerumAffectMixin {
    private static final Identifier ACTION = Identifier.of("sparkstrength", "professor_serum");
    private static final double FEED_RANGE = 1.5D;

    @Inject(method = "useHeldSerum", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardLookedAtSerumTarget(
            ServerPlayerEntity actor,
            ItemStack stack,
            @Coerce Object type,
            CallbackInfoReturnable<Boolean> cir
    ) {
        HitResult hit = ProjectileUtil.getCollision(
                actor,
                entity -> entity instanceof ServerPlayerEntity target && target != actor,
                FEED_RANGE
        );
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof ServerPlayerEntity target
                && !PlayerAffectMixinGuard.allows(actor, target, ACTION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useHeldSerumOnTarget", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardDirectSerumTarget(
            ServerPlayerEntity actor,
            ServerPlayerEntity target,
            ItemStack stack,
            @Coerce Object type,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!PlayerAffectMixinGuard.allows(actor, target, ACTION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tryRemoteFeed", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardRemoteSerumTarget(
            ServerPlayerEntity actor,
            UUID targetUuid,
            @Coerce Object type,
            CallbackInfo ci
    ) {
        ServerPlayerEntity target = PlayerAffectMixinGuard.onlineTarget(actor, targetUuid);
        if (target != null && !PlayerAffectMixinGuard.allows(actor, target, ACTION)) {
            ci.cancel();
        }
    }
}
