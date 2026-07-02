package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionWinContext;
import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class FactionWinService {
    private static boolean registered;
    private static boolean winHookRegistered;

    private FactionWinService() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        // Register after mod initialization so add-on neutral blockers run before custom faction wins.
        // 在所有模组初始化后注册，避免自定义阵营胜利抢在其他附属模组的中立阻止者之前结算。
        ServerLifecycleEvents.SERVER_STARTING.register(server -> registerWinHook());
    }

    private static void registerWinHook() {
        if (winHookRegistered) {
            return;
        }
        winHookRegistered = true;
        CheckWinCondition.EVENT.register(FactionWinService::checkWin);
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        SparkFactionRoundEndComponent roundEnd = SparkFactionRoundEndComponent.KEY.get(world.getScoreboard());
        List<FactionWinResult> results = new ArrayList<>();

        for (FactionDefinition faction : FactionRegistryImpl.getCustomFactions().stream()
                .sorted(Comparator.comparingInt(FactionDefinition::priority).reversed())
                .toList()) {
            results.add(faction.winCondition().checkWin(
                    new FactionWinContext(world, gameComponent, currentStatus, faction.id())
            ));
        }

        FactionWinResult result = chooseResult(results);
        if (result.type() == FactionWinResult.Type.BLOCK) {
            return CheckWinCondition.WinResult.block();
        }
        if (result.type() == FactionWinResult.Type.FACTION_WIN && result.winningFaction() != null) {
            Identifier winningFaction = result.winningFaction();
            roundEnd.setCustomWin(winningFaction, collectFactionWinners(world, gameComponent, winningFaction));
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.NEUTRAL);
        }

        if (currentStatus != GameFunctions.WinStatus.NEUTRAL) {
            roundEnd.clearCustomWin();
        }
        return null;
    }

    static FactionWinResult chooseResult(List<FactionWinResult> results) {
        Identifier firstWinningFaction = null;
        for (FactionWinResult result : results) {
            if (result == null || result.type() == FactionWinResult.Type.NONE) {
                continue;
            }
            if (result.type() == FactionWinResult.Type.BLOCK) {
                return FactionWinResult.block();
            }
            if (result.type() == FactionWinResult.Type.FACTION_WIN
                    && result.winningFaction() != null
                    && firstWinningFaction == null) {
                firstWinningFaction = result.winningFaction();
            }
        }
        return firstWinningFaction == null
                ? FactionWinResult.none()
                : FactionWinResult.factionWin(firstWinningFaction);
    }

    public static Set<UUID> collectFactionWinners(
            ServerWorld world,
            GameWorldComponent gameComponent,
            Identifier factionId
    ) {
        return collectFactionWinners(gameComponent.getAllPlayers(), uuid -> {
            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (player != null && gameComponent.hasAnyRole(player)) {
                return FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent);
            }
            return FactionRegistryImpl.resolveBaseFaction(gameComponent.getRole(uuid));
        }, factionId);
    }

    static Set<UUID> collectFactionWinners(
            Iterable<UUID> roundPlayers,
            Function<UUID, Identifier> factionResolver,
            Identifier factionId
    ) {
        LinkedHashSet<UUID> winners = new LinkedHashSet<>();
        for (UUID uuid : roundPlayers) {
            if (factionId.equals(factionResolver.apply(uuid))) {
                winners.add(uuid);
            }
        }
        return winners;
    }
}
