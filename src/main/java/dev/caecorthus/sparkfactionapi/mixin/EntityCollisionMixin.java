package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityCollisionMixin {
    @WrapMethod(method = "collidesWith(Lnet/minecraft/entity/Entity;)Z")
    private boolean sparkfactionapi$cancelExemptCollision(Entity other, Operation<Boolean> original) {
        // This wrapper must veto outside Wathe's player-collision wrapper, which can return true without delegating.
        // 此包装必须在 Wathe 玩家碰撞包装的外层否决，因为后者可能不委托原方法而直接返回 true。
        if (EntityCollisionExemptions.shouldCancelCollision((Entity) (Object) this, other)) {
            return false;
        }
        return original.call(other);
    }
}
