package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionInstinctRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionInstinctRulesTest {
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
    void customFactionRolesDefaultToNoInstinct() {
        Identifier factionId = Identifier.of("sparkwitch", "instinct_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "instinct_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionInstinctRules.canUseInstinct(role));
        assertEquals(-1, FactionInstinctRules.instinctColor(role));
    }

    @Test
    void legacyKillerInstinctFallbackStaysCompatible() {
        assertTrue(FactionInstinctRules.canUseInstinct(WatheRoles.KILLER));
        assertEquals(0x990000, FactionInstinctRules.displayColor(WatheRoles.KILLER));
    }

    @Test
    void instinctDisplayColorFallsBackToFactionColor() {
        Identifier factionId = Identifier.of("sparkwitch", "purple_sight");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .color(0x663399)
                .capabilities(FactionCapabilities.builder()
                        .canUseInstinct(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "purple_sight_witch"),
                        factionId
                )
                .build());

        assertEquals(0x663399, FactionInstinctRules.displayColor(role));
    }

    @Test
    void instinctColorOverridesFactionColor() {
        Identifier factionId = Identifier.of("sparkwitch", "direct_sight");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .color(0x663399)
                .capabilities(FactionCapabilities.builder()
                        .canUseInstinct(true)
                        .instinctColor(0x112233)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "direct_sight_witch"),
                        factionId
                )
                .build());

        assertEquals(0x112233, FactionInstinctRules.instinctColor(role));
        assertEquals(0x112233, FactionInstinctRules.displayColor(role));
    }

    @Test
    void customInstinctHighlightFallsThroughForDeadSpectators() {
        assertFalse(FactionInstinctRules.shouldUseCustomHighlight(false, true));
        assertTrue(FactionInstinctRules.shouldUseCustomHighlight(true, false));
        assertTrue(FactionInstinctRules.shouldUseCustomHighlight(false, false));
    }

    @Test
    void instinctPolicyResultUsesHighestPriorityNonNullResult() {
        SparkFactionApi.registerInstinctPolicy((viewer, target, gameComponent) -> null);
        SparkFactionApi.registerInstinctPolicy((viewer, target, gameComponent) ->
                FactionInstinctPolicy.InstinctResult.show(0x111111, true, 10));
        SparkFactionApi.registerInstinctPolicy((viewer, target, gameComponent) ->
                FactionInstinctPolicy.InstinctResult.show(0x222222, false, 5));
        SparkFactionApi.registerInstinctPolicy((viewer, target, gameComponent) ->
                FactionInstinctPolicy.InstinctResult.skip(20));

        Optional<FactionInstinctPolicy.InstinctResult> result = FactionInstinctRules.policyResult(null, null, null);

        assertTrue(result.isPresent());
        assertTrue(result.get().skip());
        assertEquals(20, result.get().priority());
    }

    @Test
    void registryTestResetClearsInstinctPolicies() {
        SparkFactionApi.registerInstinctPolicy((viewer, target, gameComponent) ->
                FactionInstinctPolicy.InstinctResult.show(0x111111, true, 10));

        assertTrue(FactionInstinctRules.policyResult(null, null, null).isPresent());

        FactionRegistryImpl.clearForTests();

        assertFalse(FactionInstinctRules.policyResult(null, null, null).isPresent());
    }
}
