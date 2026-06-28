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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
            roundEnd.setCustomWin(winningFaction, collectLivingFactionMembers(world, gameComponent, winningFaction));
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

    public static Set<UUID> collectLivingFactionMembers(
            ServerWorld world,
            GameWorldComponent gameComponent,
            Identifier factionId
    ) {
        LinkedHashSet<UUID> winners = new LinkedHashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            if (FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent).equals(factionId)) {
                winners.add(player.getUuid());
            }
        }

        // If every member died at win resolution time, still mark same-faction registered players.
        // 如果胜利结算时成员已死亡，仍按本局角色记录标记同阵营成员，避免团队胜利丢失。
        if (winners.isEmpty()) {
            for (UUID uuid : gameComponent.getAllPlayers()) {
                PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player != null && FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent).equals(factionId)) {
                    winners.add(uuid);
                } else if (FactionRegistryImpl.resolveBaseFaction(gameComponent.getRole(uuid)).equals(factionId)) {
                    winners.add(uuid);
                }
            }
        }
        return winners;
    }
}
