package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public record FactionAssignmentContext(
        ServerWorld world,
        GameWorldComponent gameComponent,
        List<ServerPlayerEntity> players,
        int totalPlayerCount,
        int availablePlayerCount,
        FactionAssignmentPhase phase
) {
}
