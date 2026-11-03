package dev.caecorthus.sparkfactionapi.impl.mapreset;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wires the lobby map reset to the server tick and lifecycle. The vote, start, copy and command seams are mixins.
 * 将大厅地图重置接入服务端 tick 与生命周期。投票、开局、复制与命令接缝由 Mixin 提供。
 */
public final class LobbyMapResetBootstrap {
    private static boolean registered;

    private LobbyMapResetBootstrap() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(LobbyMapReset::tick);
        // World keys are shared by every singleplayer save. / 所有单人存档共用同一组世界键。
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LobbyMapReset.clear());
    }
}
