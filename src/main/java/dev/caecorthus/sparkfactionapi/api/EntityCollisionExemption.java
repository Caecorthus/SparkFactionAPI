package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.entity.Entity;

/**
 * Determines whether an entity is exempt from pairwise entity collision. If either participant is
 * exempt, SparkFactionAPI rejects {@code Entity.collidesWith(Entity)} and cancels the matching
 * {@code LivingEntity.pushAway(Entity)} fallback. This contract does not change block collision,
 * projectile targeting, or vehicle-specific collision overrides.
 * 决定实体是否免于双方实体碰撞；只要任一参与实体被豁免，SparkFactionAPI 就会拒绝
 * {@code Entity.collidesWith(Entity)}，并取消对应的 {@code LivingEntity.pushAway(Entity)} 兜底推动。
 * 本契约不改变方块碰撞、投射物选取或载具自有的碰撞覆写。
 */
@FunctionalInterface
public interface EntityCollisionExemption {
    boolean isExempt(Entity entity);
}
