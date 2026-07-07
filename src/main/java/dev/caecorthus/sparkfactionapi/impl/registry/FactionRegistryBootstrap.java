package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.impl.roundend.FactionWinService;

/**
 * Owns one-time registry bootstrap wiring.
 * 持有注册表的一次性初始化接线。
 */
public final class FactionRegistryBootstrap {
    private static boolean bootstrapped;

    private FactionRegistryBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        FactionCatalog.registerLegacyFaction(FactionIds.NONE, 0xFFFFFF);
        FactionCatalog.registerLegacyFaction(FactionIds.CIVILIAN, 0x36E51B);
        FactionCatalog.registerLegacyFaction(FactionIds.KILLER, 0xC13838);
        FactionCatalog.registerLegacyFaction(FactionIds.NEUTRAL, 0xB567FF);
        FactionWinService.register();
    }

}
