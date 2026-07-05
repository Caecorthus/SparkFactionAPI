package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.command.settings.GameSettingsCommandRules;
import dev.caecorthus.sparkfactionapi.command.settings.GameSettingsCommandRules.RoleListEntry;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameSettingsCommandRulesTest {
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
    void roleListClickCommandsUseRegisteredWatheGameSettingsCommand() {
        String disableAccomplice = GameSettingsCommandRules.roleToggleCommand("accomplice", true);
        String enableGrandWitch = GameSettingsCommandRules.roleToggleCommand("grand_witch", false);
        String disablePigGod = GameSettingsCommandRules.roleToggleCommand("pig_god", true);

        assertEquals("/wathe:gameSettings set enableRole accomplice false", disableAccomplice);
        assertEquals("/wathe:gameSettings set enableRole grand_witch true", enableGrandWitch);
        assertEquals("/wathe:gameSettings set enableRole pig_god false", disablePigGod);
        assertFalse(disableAccomplice.startsWith("/game settings roles"));
        assertFalse(enableGrandWitch.startsWith("/game settings roles"));
        assertFalse(disablePigGod.startsWith("/game settings roles"));
    }

    @Test
    void roleListMessageSkipsSpecialRolesAndKeepsWatheToggleTextBehavior() {
        List<RoleListEntry> entries = GameSettingsCommandRules.roleListEntries(
                List.of(WatheRoles.CIVILIAN, WatheRoles.LOOSE_END, WatheRoles.KILLER),
                Set.of(WatheRoles.LOOSE_END),
                role -> role == WatheRoles.CIVILIAN
        );

        assertEquals(2, entries.size());

        RoleListEntry civilian = entries.getFirst();
        assertEquals(WatheRoles.CIVILIAN, civilian.role());
        assertEquals(0x36E51B, civilian.factionColor());
        assertEquals("faction.wathe.civilian", civilian.factionTranslationKey());
        assertEquals("announcement.role.civilian", civilian.roleTranslationKey());
        assertEquals("/wathe:gameSettings set enableRole civilian false", civilian.clickCommand());
        assertEquals("commands.wathe.listroles.click_to_disable", civilian.hoverTranslationKey());
        assertEquals("commands.wathe.listroles.enabled", civilian.enabledTranslationKey());
        assertEquals(0x55FF55, civilian.enabledColor());

        RoleListEntry killer = entries.get(1);
        assertEquals(WatheRoles.KILLER, killer.role());
        assertEquals(0xC13838, killer.factionColor());
        assertEquals("faction.wathe.killer", killer.factionTranslationKey());
        assertEquals("announcement.role.killer", killer.roleTranslationKey());
        assertEquals("/wathe:gameSettings set enableRole killer true", killer.clickCommand());
        assertEquals("commands.wathe.listroles.click_to_enable", killer.hoverTranslationKey());
        assertEquals("commands.wathe.listroles.disabled", killer.enabledTranslationKey());
        assertEquals(0xFF5555, killer.enabledColor());
    }

    @Test
    void roleListMessageUsesCustomFactionDefinitionTextWhenRoleBelongsToCustomFaction() {
        Identifier factionId = Identifier.of("sparkwitch", "witch");
        SparkFactionApi.registerFaction(FactionDefinition.builder(factionId)
                .color(0x7D2AFF)
                .translationKeyPrefix("faction.sparkwitch.witch")
                .build());
        Role highWitch = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "high_witch"),
                        factionId
                )
                .color(0x7D2AFF)
                .build());

        List<RoleListEntry> entries = GameSettingsCommandRules.roleListEntries(
                List.of(highWitch),
                Set.of(),
                role -> false
        );

        RoleListEntry entry = entries.getFirst();
        assertEquals(highWitch, entry.role());
        assertEquals(0x7D2AFF, entry.factionColor());
        assertEquals("faction.sparkwitch.witch", entry.factionTranslationKey());
        assertEquals("announcement.role.high_witch", entry.roleTranslationKey());
        assertEquals("/wathe:gameSettings set enableRole high_witch true", entry.clickCommand());
    }
}
