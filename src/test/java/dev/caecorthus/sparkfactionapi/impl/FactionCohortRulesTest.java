package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionCohortRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionCohortRulesTest {
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
    void customFactionRolesDefaultToNoCohortSharing() {
        Identifier factionId = Identifier.of("sparkwitch", "cohort_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "cohort_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionCohortRules.sharesCohort(role, role));
    }

    @Test
    void legacyKillerCohortFallbackStaysCompatible() {
        assertTrue(FactionCohortRules.sharesCohort(WatheRoles.KILLER, WatheRoles.KILLER));
    }

    @Test
    void cohortSharingRequiresMatchingFactionAndCapabilityOnBothSides() {
        Identifier viewerFaction = Identifier.of("sparkwitch", "shared_sight");
        Identifier targetFaction = Identifier.of("sparkwitch", "other_sight");
        SparkFactionApi.registerFaction(FactionDefinition.builder(viewerFaction)
                .capabilities(FactionCapabilities.builder()
                        .sharesCohort(true)
                        .build())
                .build());
        SparkFactionApi.registerFaction(FactionDefinition.builder(targetFaction)
                .capabilities(FactionCapabilities.builder()
                        .sharesCohort(true)
                        .build())
                .build());
        Role viewer = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "shared_viewer"),
                        viewerFaction
                )
                .build());
        Role ally = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "shared_ally"),
                        viewerFaction
                )
                .build());
        Role outsider = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "shared_outsider"),
                        targetFaction
                )
                .build());

        assertTrue(FactionCohortRules.sharesCohort(viewer, ally));
        assertFalse(FactionCohortRules.sharesCohort(viewer, outsider));
    }
}
