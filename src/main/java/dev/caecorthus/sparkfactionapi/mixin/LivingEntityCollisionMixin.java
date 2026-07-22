package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCollisionMixin {
    @Inject(
            method = "pushAway(Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparkfactionapi$cancelExemptPush(Entity other, CallbackInfo ci) {
        if (EntityCollisionExemptions.shouldCancelPush((Entity) (Object) this, other)) {
            ci.cancel();
        }
    }
}
