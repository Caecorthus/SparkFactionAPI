package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.entity.Entity;

/**
 * Determines whether an entity is exempt from a physical push initiated by a living entity. If
 * either participant is exempt, SparkFactionAPI cancels that {@code LivingEntity.pushAway(Entity)}
 * operation. This contract does not cover vehicle-specific collision overrides.
 * 决定实体是否免受生物发起的物理推动；只要任一参与实体被豁免，SparkFactionAPI 就会取消该次
 * {@code LivingEntity.pushAway(Entity)} 操作。本契约不覆盖载具自有的碰撞覆写。
 */
@FunctionalInterface
public interface EntityCollisionExemption {
    boolean isExempt(Entity entity);
}
