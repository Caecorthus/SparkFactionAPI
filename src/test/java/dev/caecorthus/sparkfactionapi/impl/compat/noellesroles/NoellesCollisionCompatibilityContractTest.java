package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesCollisionCompatibilityContractTest {
    @Test
    void registersOptionalNoCollisionEffectWithoutProviderLinkage() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/compat/noellesroles/NoellesCollisionCompatibility.java"
        ));
        String initializer = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/SparkFactionApiMod.java"
        ));

        assertTrue(source.contains("Identifier.of(\"noellesroles\", \"no_collision\")"));
        assertFalse(source.contains("Identifier.of(\"noellesroles\", \"no_collisions\")"));
        assertTrue(source.contains("SparkFactionApi.registerEntityCollisionExemption"));
        assertTrue(source.contains("entity instanceof LivingEntity"));
        assertTrue(source.contains("Registries.STATUS_EFFECT.getEntry"));
        assertTrue(source.contains("livingEntity.hasStatusEffect"));
        assertFalse(source.contains("org.agmas.noellesroles"));
        assertTrue(initializer.contains("NoellesCollisionCompatibility.register();"));
    }
}
