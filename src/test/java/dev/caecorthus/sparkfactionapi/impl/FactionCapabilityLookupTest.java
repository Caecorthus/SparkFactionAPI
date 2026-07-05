package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionCapabilityLookupTest {
    @BeforeEach
    void setUp() {
        FactionRegistryImpl.clearForTests();
        SparkFactionApi.bootstrap();
    }

    @AfterEach
    void tearDown() {
        FactionRegistryImpl.clearForTests();
    }

    @Test
    void baseRoleLookupReturnsRegisteredFactionCapabilities() {
        Identifier factionId = Identifier.of("sparkwitch", "lookup_witch");
        Identifier targetTag = Identifier.of("sparkwitch", "victim");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .canUseInstinct(true)
                        .targetTag(targetTag)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "lookup_witch_role"),
                        factionId
                )
                .build());

        assertEquals(factionId, FactionCapabilityLookup.baseFaction(role));
        assertTrue(FactionCapabilityLookup.hasCustomBaseFaction(role));
        assertTrue(FactionCapabilityLookup.capabilities(role).canUseInstinct());
        assertTrue(FactionCapabilityLookup.capabilities(role).targetTags().contains(targetTag));
    }

    @Test
    void legacyFallbackStillRoutesThroughBaseFaction() {
        assertEquals(FactionIds.KILLER, FactionCapabilityLookup.baseFaction(WatheRoles.KILLER));
        assertTrue(FactionCapabilityLookup.capabilities(WatheRoles.KILLER).canUseKillerFeatures());
        assertFalse(FactionCapabilityLookup.hasCustomBaseFaction(WatheRoles.KILLER));
        assertEquals(FactionCapabilities.none(), FactionCapabilityLookup.capabilities(WatheRoles.NO_ROLE));
    }

    @Test
    void nullEffectiveContextUsesNoFactionCapabilitiesAndNoCustomFaction() {
        assertEquals(FactionIds.NONE, FactionCapabilityLookup.effectiveFaction(null, null));
        assertEquals(FactionCapabilities.none(), FactionCapabilityLookup.capabilities(null, null));
        assertFalse(FactionCapabilityLookup.hasCustomEffectiveFaction(null, null));
    }
}
