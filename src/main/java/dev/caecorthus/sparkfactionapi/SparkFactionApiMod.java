package dev.caecorthus.sparkfactionapi;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.command.SparkFactionAdminCommands;
import dev.caecorthus.sparkfactionapi.impl.FactionCompatibilityEvents;
import dev.caecorthus.sparkfactionapi.net.SparkFactionVersionHandshake;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;

public final class SparkFactionApiMod implements ModInitializer {
    public static final String MOD_ID = "sparkfactionapi";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        SparkFactionVersionHandshake.registerServer();
        SparkFactionApi.bootstrap();
        FactionCompatibilityEvents.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SparkFactionAdminCommands.register(dispatcher));
    }
}
