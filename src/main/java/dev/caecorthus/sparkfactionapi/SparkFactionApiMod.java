package dev.caecorthus.sparkfactionapi;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.FactionCompatibilityEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public final class SparkFactionApiMod implements ModInitializer {
    public static final String MOD_ID = "sparkfactionapi";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        SparkFactionApi.bootstrap();
        FactionCompatibilityEvents.register();
    }
}
