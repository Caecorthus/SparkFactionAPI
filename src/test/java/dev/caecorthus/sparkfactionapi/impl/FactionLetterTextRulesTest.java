package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.text.FactionLetterTextRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionLetterTextRulesTest {
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
    void legacyWatheFactionKeepsOriginalLetterFallback() {
        assertEquals(
                "wathe.killer",
                FactionLetterTextRules.letterFactionName(WatheRoles.KILLER, "wathe.killer")
        );
    }

    @Test
    void customFactionUsesNamespaceAndDotSeparatedPath() {
        Identifier covenFaction = Identifier.of("sparkwitch", "deep/coven");
        SparkFactionApi.registerFaction(FactionDefinition.builder(covenFaction).build());
        Role covenRole = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "deep_coven_member"),
                        covenFaction
                )
                .build());

        assertEquals(
                "sparkwitch.deep.coven",
                FactionLetterTextRules.letterFactionName(covenRole, "wathe.neutral")
        );
    }
}
