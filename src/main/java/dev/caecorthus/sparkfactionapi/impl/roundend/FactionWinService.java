package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

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
        FactionWinResult result = FactionWinRules.customWinResult(
                FactionRegistryImpl.getCustomFactions(),
                world,
                gameComponent,
                currentStatus
        );
        if (result.type() == FactionWinResult.Type.BLOCK) {
            return CheckWinCondition.WinResult.block();
        }
        if (result.type() == FactionWinResult.Type.FACTION_WIN && result.winningFaction() != null) {
            Identifier winningFaction = result.winningFaction();
            roundEnd.setCustomWin(
                    winningFaction,
                    FactionWinnerCollector.collectFactionWinners(world, gameComponent, winningFaction)
            );
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.NEUTRAL);
        }

        if (currentStatus != GameFunctions.WinStatus.NEUTRAL) {
            roundEnd.clearCustomWin();
        }
        return null;
    }
}
