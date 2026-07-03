package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.GameSettingsCommandRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameSettingsCommandMixinTest {
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
}
