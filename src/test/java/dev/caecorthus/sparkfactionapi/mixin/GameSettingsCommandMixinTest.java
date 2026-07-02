package dev.caecorthus.sparkfactionapi.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameSettingsCommandMixinTest {
    @Test
    void roleListClickCommandsUseRegisteredWatheGameSettingsCommand() {
        String disableAccomplice = GameSettingsCommandMixin.roleToggleCommand("accomplice", true);
        String enableGrandWitch = GameSettingsCommandMixin.roleToggleCommand("grand_witch", false);
        String disablePigGod = GameSettingsCommandMixin.roleToggleCommand("pig_god", true);

        assertEquals("/wathe:gameSettings set enableRole accomplice false", disableAccomplice);
        assertEquals("/wathe:gameSettings set enableRole grand_witch true", enableGrandWitch);
        assertEquals("/wathe:gameSettings set enableRole pig_god false", disablePigGod);
        assertFalse(disableAccomplice.startsWith("/game settings roles"));
        assertFalse(enableGrandWitch.startsWith("/game settings roles"));
        assertFalse(disablePigGod.startsWith("/game settings roles"));
    }
}
