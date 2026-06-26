package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
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

class FactionRegistryImplTest {
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
    void legacyWatheRolesKeepTheirFallbackFactions() {
        assertEquals(FactionIds.NONE, SparkFactionApi.resolveBaseFaction(WatheRoles.NO_ROLE));
        assertEquals(FactionIds.CIVILIAN, SparkFactionApi.resolveBaseFaction(WatheRoles.CIVILIAN));
        assertEquals(FactionIds.KILLER, SparkFactionApi.resolveBaseFaction(WatheRoles.KILLER));
        assertEquals(FactionIds.NEUTRAL, SparkFactionApi.resolveBaseFaction(WatheRoles.LOOSE_END));
    }

    @Test
    void customRolesResolveToTheirRegisteredFaction() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction)
                .color(0x7D2AFF)
                .build());

        Role highWitch = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "high_witch"),
                        witchFaction
                )
                .color(0x7D2AFF)
                .build());

        assertEquals(witchFaction, SparkFactionApi.resolveBaseFaction(highWitch));
        assertFalse(highWitch.isInnocent());
        assertFalse(highWitch.canUseKiller());
    }

    @Test
    void customFactionCapabilitiesDefaultOff() {
        Identifier witchFaction = Identifier.of("sparkwitch", "silent_witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(witchFaction).build());

        FactionCapabilities capabilities = SparkFactionApi.capabilities(witchFaction);

        assertFalse(capabilities.canUseKillerFeatures());
        assertFalse(capabilities.receivesKillerPassiveMoney());
        assertFalse(capabilities.receivesKillRewards());
        assertFalse(capabilities.isPunishableInnocentGunVictim());
        assertFalse(capabilities.isPunishableInnocentGunShooter());
        assertFalse(capabilities.sharesCohort());
        assertFalse(capabilities.canUseInstinct());
        assertTrue(capabilities.targetTags().isEmpty());
    }

    @Test
    void defaultTranslationPrefixUsesResourceKeySafeIdentifierPath() {
        FactionDefinition faction = FactionDefinition.builder(Identifier.of("sparkwitch", "coven/witches")).build();

        assertEquals("faction.sparkwitch.coven.witches", faction.translationKeyPrefix());
    }
}
