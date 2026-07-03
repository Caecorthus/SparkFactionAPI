package dev.caecorthus.sparkfactionapi.client.net;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.net.SparkFactionVersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;

import java.util.concurrent.CompletableFuture;

public final class SparkFactionClientVersionHandshake {
    private static boolean clientRegistered;

    private SparkFactionClientVersionHandshake() {
    }

    public static synchronized void registerClient() {
        if (clientRegistered) {
            SparkFactionApiMod.LOGGER.info("SparkFactionAPI client login version receiver is already registered.");
            return;
        }
        clientRegistered = true;

        boolean registered = ClientLoginNetworking.registerGlobalReceiver(SparkFactionVersionHandshake.VERSION_CHECK_ID,
                (client, handler, buf, callbacks) -> {
                    String serverVersion = SparkFactionVersionHandshake.readVersion(buf);
                    String clientVersion = SparkFactionVersionHandshake.localVersion();
                    SparkFactionApiMod.LOGGER.info(
                            "Answering SparkFactionAPI login version query: server={}, client={}.",
                            serverVersion,
                            clientVersion
                    );
                    return CompletableFuture.completedFuture(
                            SparkFactionVersionHandshake.writeVersion(clientVersion));
                });
        if (registered) {
            // The server treats an unregistered receiver as a missing client-side mod.
            // 服务端会把未注册的接收器判定为客户端缺少该模组。
            SparkFactionApiMod.LOGGER.info(
                    "Registered SparkFactionAPI client login version receiver on channel {}.",
                    SparkFactionVersionHandshake.VERSION_CHECK_ID
            );
        } else {
            SparkFactionApiMod.LOGGER.warn(
                    "SparkFactionAPI client login version receiver already existed on channel {}.",
                    SparkFactionVersionHandshake.VERSION_CHECK_ID
            );
        }
    }
}
