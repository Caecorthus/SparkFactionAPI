package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Bridges NoellesRoles' status-effect capability into the shared bilateral collision contract. */
public final class NoellesCollisionCompatibility {
    private static final Identifier NO_COLLISION = Identifier.of("noellesroles", "no_collision");
    private static boolean registered;

    private NoellesCollisionCompatibility() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        SparkFactionApi.registerEntityCollisionExemption(entity -> {
            if (!(entity instanceof LivingEntity livingEntity)) {
                return false;
            }
            return Registries.STATUS_EFFECT.getEntry(NO_COLLISION)
                    .map(effect -> livingEntity.hasStatusEffect(effect))
                    .orElse(false);
        });
    }
}
