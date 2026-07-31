package dev.caecorthus.sparkfactionapi.impl.collision;

import dev.caecorthus.sparkfactionapi.api.EntityCollisionExemption;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

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
        return entity instanceof EntityCollisionTrackedState trackedState
                && trackedState.sparkfactionapi$isCollisionExempt();
    }

    public static void refresh(Entity entity) {
        if (entity == null || !(entity.getWorld() instanceof ServerWorld)) {
            return;
        }
        if (!(entity instanceof EntityCollisionTrackedState trackedState)) {
            return;
        }

        boolean exempt = evaluateProviders(entity);
        if (trackedState.sparkfactionapi$isCollisionExempt() != exempt) {
            trackedState.sparkfactionapi$setCollisionExempt(exempt);
        }
    }

    static boolean evaluateProviders(Entity entity) {
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
