package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionWinContext;
import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Owns custom faction win-result ordering and aggregation.
 * 持有自定义阵营胜利结果的排序与聚合。
 */
public final class FactionWinRules {
    private FactionWinRules() {
    }

    public static FactionWinResult customWinResult(
            Collection<FactionDefinition> factions,
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        List<FactionWinResult> results = new ArrayList<>();
        for (FactionDefinition faction : factions.stream()
                .sorted(Comparator.comparingInt(FactionDefinition::priority).reversed())
                .toList()) {
            results.add(faction.winCondition().checkWin(
                    new FactionWinContext(world, gameComponent, currentStatus, faction.id())
            ));
        }
        return chooseResult(results);
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
}
