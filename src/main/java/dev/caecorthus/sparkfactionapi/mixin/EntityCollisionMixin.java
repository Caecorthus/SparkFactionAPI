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
        // 以高于 Wathe 默认 1000 的优先级包裹 collidesWith，先否决豁免碰撞再进入玩家硬碰撞逻辑。
        // 这里仍然只做本地即时判定，不注册 Entity/LivingEntity/PlayerEntity 的 DataTracker 字段，避免协议编号错位。
        if (EntityCollisionExemptions.shouldCancelCollision((Entity) (Object) this, other)) {
            return false;
        }
        return original.call(other);
    }
}
