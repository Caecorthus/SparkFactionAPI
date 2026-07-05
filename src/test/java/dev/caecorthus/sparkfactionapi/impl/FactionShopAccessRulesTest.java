package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.shop.FactionShopAccessRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionShopAccessRulesTest {
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
    void customFactionRolesDefaultToNoKillerShopAccess() {
        Identifier factionId = Identifier.of("sparkwitch", "shop_safe");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId).build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "shop_safe_witch"),
                        factionId
                )
                .build());

        assertFalse(FactionShopAccessRules.canUseKillerShop(role));
    }

    @Test
    void legacyKillerShopAccessStaysCompatible() {
        assertTrue(FactionShopAccessRules.canUseKillerShop(WatheRoles.KILLER));
    }

    @Test
    void customFactionCanOptIntoKillerShopAccess() {
        Identifier factionId = Identifier.of("sparkwitch", "shop_enabled");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .capabilities(FactionCapabilities.builder()
                        .canUseKillerFeatures(true)
                        .build())
                .build());
        Role role = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "shop_enabled_witch"),
                        factionId
                )
                .build());

        assertTrue(FactionShopAccessRules.canUseKillerShop(role));
    }
}
