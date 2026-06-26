package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public record FactionWinContext(
        ServerWorld world,
        GameWorldComponent gameComponent,
        GameFunctions.WinStatus currentStatus,
        Identifier factionId
) {
}
