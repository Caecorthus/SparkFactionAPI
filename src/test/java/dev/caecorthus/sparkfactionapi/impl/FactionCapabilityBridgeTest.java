package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionCapabilityBridgeTest {
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
    void customFactionRolesDefaultToNoNativeCapabilities() {
        Identifier factionId = Identifier.of("sparkwitch", "capability_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "capability_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionCapabilityBridge.canUseKillerFeatureAccess(role));
        assertFalse(FactionCapabilityBridge.receivesKillerPassiveMoney(role));
        assertFalse(FactionCapabilityBridge.receivesKillReward(role));
        assertFalse(FactionCapabilityBridge.isPunishableInnocentGunVictim(role));
        assertFalse(FactionCapabilityBridge.isPunishableInnocentGunShooter(role));
        assertFalse(FactionCapabilityBridge.sharesCohort(role, role));
        assertFalse(FactionCapabilityBridge.canUseInstinct(role));
    }

    @Test
    void legacyFallbackCapabilitiesStayCompatible() {
        assertTrue(FactionCapabilityBridge.canUseKillerFeatureAccess(WatheRoles.KILLER));
        assertTrue(FactionCapabilityBridge.receivesKillerPassiveMoney(WatheRoles.KILLER));
        assertTrue(FactionCapabilityBridge.receivesKillReward(WatheRoles.KILLER));
        assertTrue(FactionCapabilityBridge.sharesCohort(WatheRoles.KILLER, WatheRoles.KILLER));
        assertTrue(FactionCapabilityBridge.canUseInstinct(WatheRoles.KILLER));

        assertTrue(FactionCapabilityBridge.isPunishableInnocentGunVictim(WatheRoles.CIVILIAN));
        assertTrue(FactionCapabilityBridge.isPunishableInnocentGunShooter(WatheRoles.CIVILIAN));
    }

    @Test
    void targetTagsGateBaseTargetEligibility() {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");
        Identifier viewerFaction = Identifier.of("sparkwitch", "hex_viewer");
        Identifier targetFaction = Identifier.of("sparkwitch", "hex_target_pool");
        SparkFactionApi.registerFaction(FactionDefinition.builder(viewerFaction).build());
        SparkFactionApi.registerFaction(FactionDefinition.builder(targetFaction)
                .capabilities(dev.caecorthus.sparkfactionapi.api.FactionCapabilities.builder()
                        .targetTag(targetTag)
                        .build())
                .build());
        Role viewer = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "hex_viewer_role"),
                        viewerFaction
                )
                .build());
        Role target = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "hex_target_role"),
                        targetFaction
                )
                .build());

        assertTrue(FactionCapabilityBridge.canTarget(viewer, target, targetTag));
        assertFalse(FactionCapabilityBridge.canTarget(target, viewer, targetTag));
    }

    @Test
    void instinctDisplayColorFallsBackToFactionColor() {
        Identifier factionId = Identifier.of("sparkwitch", "purple_sight");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .color(0x663399)
                .capabilities(dev.caecorthus.sparkfactionapi.api.FactionCapabilities.builder()
                        .canUseInstinct(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "purple_sight_witch"),
                        factionId
                )
                .build());

        assertEquals(0x663399, FactionCapabilityBridge.instinctDisplayColor(role));
    }
}
