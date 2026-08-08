package dev.caecorthus.sparkfactionapi.impl.collision;

import dev.caecorthus.sparkfactionapi.api.EntityCollisionExemption;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EntityCollisionExemptions {
    private static final List<EntityCollisionExemption> EXEMPTIONS = new ArrayList<>();

    private EntityCollisionExemptions() {
    }

    public static void register(EntityCollisionExemption exemption) {
        EXEMPTIONS.add(Objects.requireNonNull(exemption, "exemption"));
    }

    public static boolean isExempt(Entity entity) {
        // 回退为本地即时判定：不要再给 Entity 注册额外 DataTracker 字段。
        // 客户端带 DLC、大厅服未带 DLC 时，新增实体同步字段会把原版字段编号顶偏，
        // 最终在 clientbound/minecraft:set_entity_data 包里触发类型不一致的网络协议错误。
        if (entity == null) {
            return false;
        }
        for (EntityCollisionExemption exemption : EXEMPTIONS) {
            if (exemption.isExempt(entity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldCancelCollision(Entity self, Entity other) {
        return shouldCancelCollision(isExempt(self), isExempt(other));
    }

    public static boolean shouldCancelPush(Entity self, Entity other) {
        return shouldCancelCollision(self, other);
    }

    static boolean shouldCancelCollision(boolean selfExempt, boolean otherExempt) {
        return selfExempt || otherExempt;
    }
}
