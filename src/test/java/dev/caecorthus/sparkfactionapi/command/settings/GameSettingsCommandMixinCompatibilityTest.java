package dev.caecorthus.sparkfactionapi.command.settings;

import dev.doctor4t.wathe.command.GameSettingsCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsCommandMixinCompatibilityTest {
    private static final Path MIXIN_SOURCE = Path.of(
            "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameSettingsCommandMixin.java"
    );
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/sparkfactionapi.mixins.json");

    @Test
    void wathe156ListRolesDescriptorStillMatchesRequiredInjection() throws Exception {
        assertEquals(
                int.class,
                GameSettingsCommand.class
                        .getDeclaredMethod("listRoles", ServerCommandSource.class)
                        .getReturnType()
        );

        String source = Files.readString(MIXIN_SOURCE);
        assertTrue(source.contains(
                "method = \"listRoles(Lnet/minecraft/server/command/ServerCommandSource;)I\""
        ));
        assertTrue(source.contains("require = 1"));
    }

    @Test
    void listRolesMixinRemainsConfiguredInRequiredCommonMixinSet() throws Exception {
        String config = Files.readString(MIXIN_CONFIG);
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"GameSettingsCommandMixin\""));

        String source = Files.readString(MIXIN_SOURCE);
        assertTrue(source.contains("WatheRoles.ROLES"));
        assertTrue(source.contains("WatheRoles.SPECIAL_ROLES"));
    }
}
