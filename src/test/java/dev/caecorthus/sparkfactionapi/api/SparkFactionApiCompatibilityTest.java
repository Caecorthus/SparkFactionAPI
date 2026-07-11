package dev.caecorthus.sparkfactionapi.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

class SparkFactionApiCompatibilityTest {
    @Test
    void roleOnlyEffectiveResolverIsMarkedAsLegacyBaseFactionAlias() throws NoSuchMethodException {
        Deprecated deprecation = SparkFactionApi.class
                .getDeclaredMethod("resolveEffectiveFaction", Role.class)
                .getAnnotation(Deprecated.class);

        assertNotNull(deprecation);
        assertFalse(deprecation.forRemoval());
    }
}
