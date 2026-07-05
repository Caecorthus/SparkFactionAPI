package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.economy.FactionEconomyRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionEconomyRulesTest {
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
    void customFactionRolesDefaultToNoKillerEconomy() {
        Identifier factionId = Identifier.of("sparkwitch", "economy_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "economy_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionEconomyRules.receivesKillerPassiveMoney(role));
        assertFalse(FactionEconomyRules.receivesKillReward(role));
    }

    @Test
    void legacyKillerFallbackCapabilitiesStayCompatible() {
        assertTrue(FactionEconomyRules.receivesKillerPassiveMoney(WatheRoles.KILLER));
        assertTrue(FactionEconomyRules.receivesKillReward(WatheRoles.KILLER));
    }

    @Test
    void customFactionCanGrantKillerEconomyWithoutKillerFeatures() {
        Identifier factionId = Identifier.of("sparkwitch", "economy_reward");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .receivesKillerPassiveMoney(true)
                        .receivesKillRewards(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "economy_reward_witch"),
                        factionId
                )
                .build());

        assertTrue(FactionEconomyRules.receivesKillerPassiveMoney(role));
        assertTrue(FactionEconomyRules.receivesKillReward(role));
        assertFalse(FactionEconomyRules.receivesCustomKillerPassiveMoney(null, null));
        assertFalse(FactionEconomyRules.receivesCustomKillReward(null, null));
    }

    @Test
    void economyPolicyUsesFirstNonNullOverride() {
        SparkFactionApi.registerEconomyPolicy((player, rewardKind, gameComponent) -> null);
        SparkFactionApi.registerEconomyPolicy((player, rewardKind, gameComponent) ->
                rewardKind == FactionEconomyPolicy.RewardKind.PASSIVE ? Boolean.TRUE : Boolean.FALSE);
        SparkFactionApi.registerEconomyPolicy((player, rewardKind, gameComponent) ->
                rewardKind == FactionEconomyPolicy.RewardKind.PASSIVE ? Boolean.FALSE : Boolean.TRUE);

        assertTrue(FactionEconomyRules.receivesKillerPassiveMoney(null, null));
        assertFalse(FactionEconomyRules.receivesKillReward(null, null));
    }

    @Test
    void registryTestResetClearsEconomyPolicies() {
        SparkFactionApi.registerEconomyPolicy((player, rewardKind, gameComponent) -> Boolean.TRUE);

        assertTrue(FactionEconomyRules.receivesKillerPassiveMoney(null, null));

        FactionRegistryImpl.clearForTests();

        assertFalse(FactionEconomyRules.receivesKillerPassiveMoney(null, null));
    }
}
