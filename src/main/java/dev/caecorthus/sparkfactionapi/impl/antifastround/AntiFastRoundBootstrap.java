package dev.caecorthus.sparkfactionapi.impl.antifastround;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wires the safe-time lifecycle to Wathe round events and the server tick, and installs the interaction lock.
 * 将安全时间生命周期接入 Wathe 对局事件与服务端 tick，并安装交互锁。
 */
public final class AntiFastRoundBootstrap {
    private static boolean registered;

    private AntiFastRoundBootstrap() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        // ON_FINISH_INITIALIZE fires after roles, items, start cooldowns and teleports, while the status is STARTING.
        // ON_FINISH_INITIALIZE 在职业、物品、开局冷却与传送完成后触发，此时状态仍为 STARTING。
        GameEvents.ON_FINISH_INITIALIZE.register(AntiFastRound::onRoundInitialized);
        GameEvents.ON_FINISH_FINALIZE.register(AntiFastRound::onRoundFinalized);
        ServerTickEvents.END_SERVER_TICK.register(AntiFastRound::tick);
        AntiFastRoundGuards.register();
    }
}
