package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refreshes collision state after entity data loads and before its initial tracking packet is built.
 * 在实体数据加载完成后、初始追踪数据包构建前刷新碰撞状态。
 */
@Mixin(targets = "net.minecraft.server.world.ServerWorld$ServerEntityHandler")
public abstract class ServerEntityHandlerCollisionMixin {
    @Inject(method = "startTracking(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void sparkfactionapi$refreshCollisionBeforeTracking(Entity entity, CallbackInfo ci) {
        EntityCollisionExemptions.refresh(entity);
    }
}
