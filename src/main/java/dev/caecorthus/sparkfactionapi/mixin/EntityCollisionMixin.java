package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Inject(
            method = "collidesWith(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparkfactionapi$cancelExemptCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (EntityCollisionExemptions.shouldCancelCollision((Entity) (Object) this, other)) {
            cir.setReturnValue(false);
        }
    }
}
