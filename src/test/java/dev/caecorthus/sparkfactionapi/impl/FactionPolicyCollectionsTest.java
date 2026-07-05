package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionBlackoutCooldownPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutCooldownPolicies;
import dev.caecorthus.sparkfactionapi.impl.economy.FactionEconomyPolicies;
import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentPolicies;
import dev.caecorthus.sparkfactionapi.impl.target.FactionTargetPolicies;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionInstinctPolicies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FactionPolicyCollectionsTest {
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
    void policyCollectionsExposeReadOnlySnapshots() {
        FactionTargetEligibility targetPolicy = (viewer, target, targetTag, gameComponent) -> null;
        FactionEconomyPolicy economyPolicy = (player, rewardKind, gameComponent) -> null;
        FactionGunPunishmentPolicy gunPolicy = (player, subject, gameComponent) -> null;
        FactionBlackoutCooldownPolicy blackoutPolicy = (purchaser, target, gameComponent) -> null;
        FactionInstinctPolicy instinctPolicy = (viewer, target, gameComponent) -> null;

        SparkFactionApi.registerTargetEligibility(targetPolicy);
        SparkFactionApi.registerEconomyPolicy(economyPolicy);
        SparkFactionApi.registerGunPunishmentPolicy(gunPolicy);
        SparkFactionApi.registerBlackoutCooldownPolicy(blackoutPolicy);
        SparkFactionApi.registerInstinctPolicy(instinctPolicy);

        List<FactionTargetEligibility> targetPolicies = FactionTargetPolicies.targetEligibility();
        List<FactionEconomyPolicy> economyPolicies = FactionEconomyPolicies.economyPolicies();
        List<FactionGunPunishmentPolicy> gunPolicies = FactionGunPunishmentPolicies.gunPunishmentPolicies();
        List<FactionBlackoutCooldownPolicy> blackoutPolicies =
                FactionBlackoutCooldownPolicies.blackoutCooldownPolicies();
        List<FactionInstinctPolicy> instinctPolicies = FactionInstinctPolicies.instinctPolicies();

        assertEquals(List.of(targetPolicy), targetPolicies);
        assertEquals(List.of(economyPolicy), economyPolicies);
        assertEquals(List.of(gunPolicy), gunPolicies);
        assertEquals(List.of(blackoutPolicy), blackoutPolicies);
        assertEquals(List.of(instinctPolicy), instinctPolicies);

        assertThrows(UnsupportedOperationException.class, targetPolicies::clear);
        assertThrows(UnsupportedOperationException.class, economyPolicies::clear);
        assertThrows(UnsupportedOperationException.class, gunPolicies::clear);
        assertThrows(UnsupportedOperationException.class, blackoutPolicies::clear);
        assertThrows(UnsupportedOperationException.class, instinctPolicies::clear);
        assertThrows(UnsupportedOperationException.class, FactionRegistryImpl.targetEligibility()::clear);
        assertThrows(UnsupportedOperationException.class, FactionRegistryImpl.economyPolicies()::clear);
        assertThrows(UnsupportedOperationException.class, FactionRegistryImpl.gunPunishmentPolicies()::clear);
        assertThrows(UnsupportedOperationException.class, FactionRegistryImpl.blackoutCooldownPolicies()::clear);
        assertThrows(UnsupportedOperationException.class, FactionRegistryImpl.instinctPolicies()::clear);
    }

    @Test
    void policySnapshotsDoNotMutateWhenLaterPoliciesRegister() {
        FactionTargetEligibility firstTarget = (viewer, target, targetTag, gameComponent) -> null;
        FactionTargetEligibility secondTarget = (viewer, target, targetTag, gameComponent) -> Boolean.TRUE;
        FactionEconomyPolicy firstEconomy = (player, rewardKind, gameComponent) -> null;
        FactionEconomyPolicy secondEconomy = (player, rewardKind, gameComponent) -> Boolean.TRUE;
        FactionGunPunishmentPolicy firstGun = (player, subject, gameComponent) -> null;
        FactionGunPunishmentPolicy secondGun = (player, subject, gameComponent) -> Boolean.TRUE;
        FactionBlackoutCooldownPolicy firstBlackout = (purchaser, target, gameComponent) -> null;
        FactionBlackoutCooldownPolicy secondBlackout = (purchaser, target, gameComponent) -> Boolean.TRUE;
        FactionInstinctPolicy firstInstinct = (viewer, target, gameComponent) -> null;
        FactionInstinctPolicy secondInstinct = (viewer, target, gameComponent) ->
                FactionInstinctPolicy.InstinctResult.show(0xFFFFFF, false, 1);

        SparkFactionApi.registerTargetEligibility(firstTarget);
        SparkFactionApi.registerEconomyPolicy(firstEconomy);
        SparkFactionApi.registerGunPunishmentPolicy(firstGun);
        SparkFactionApi.registerBlackoutCooldownPolicy(firstBlackout);
        SparkFactionApi.registerInstinctPolicy(firstInstinct);
        List<FactionTargetEligibility> targetSnapshot = FactionTargetPolicies.targetEligibility();
        List<FactionEconomyPolicy> economySnapshot = FactionEconomyPolicies.economyPolicies();
        List<FactionGunPunishmentPolicy> gunSnapshot = FactionGunPunishmentPolicies.gunPunishmentPolicies();
        List<FactionBlackoutCooldownPolicy> blackoutSnapshot =
                FactionBlackoutCooldownPolicies.blackoutCooldownPolicies();
        List<FactionInstinctPolicy> instinctSnapshot = FactionInstinctPolicies.instinctPolicies();

        SparkFactionApi.registerTargetEligibility(secondTarget);
        SparkFactionApi.registerEconomyPolicy(secondEconomy);
        SparkFactionApi.registerGunPunishmentPolicy(secondGun);
        SparkFactionApi.registerBlackoutCooldownPolicy(secondBlackout);
        SparkFactionApi.registerInstinctPolicy(secondInstinct);

        assertEquals(List.of(firstTarget), targetSnapshot);
        assertEquals(List.of(firstEconomy), economySnapshot);
        assertEquals(List.of(firstGun), gunSnapshot);
        assertEquals(List.of(firstBlackout), blackoutSnapshot);
        assertEquals(List.of(firstInstinct), instinctSnapshot);
        assertEquals(List.of(firstTarget, secondTarget), FactionTargetPolicies.targetEligibility());
        assertEquals(List.of(firstEconomy, secondEconomy), FactionEconomyPolicies.economyPolicies());
        assertEquals(List.of(firstGun, secondGun), FactionGunPunishmentPolicies.gunPunishmentPolicies());
        assertEquals(List.of(firstBlackout, secondBlackout),
                FactionBlackoutCooldownPolicies.blackoutCooldownPolicies());
        assertEquals(List.of(firstInstinct, secondInstinct), FactionInstinctPolicies.instinctPolicies());
    }
}
