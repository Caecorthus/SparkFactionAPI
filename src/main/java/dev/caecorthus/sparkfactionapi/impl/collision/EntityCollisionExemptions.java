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

    public static boolean shouldCancelPush(Entity self, Entity other) {
        return shouldCancelPush(isExempt(self), isExempt(other));
    }

    /** Either side may veto the pair so callers cannot push an exempt entity from the opposite side. */
    static boolean shouldCancelPush(boolean selfExempt, boolean otherExempt) {
        return selfExempt || otherExempt;
    }
}
