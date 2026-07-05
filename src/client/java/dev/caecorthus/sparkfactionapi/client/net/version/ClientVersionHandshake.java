package dev.caecorthus.sparkfactionapi.client.net.version;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.net.version.VersionProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;

import java.util.concurrent.CompletableFuture;

public final class ClientVersionHandshake {
    private static boolean clientRegistered;

    private ClientVersionHandshake() {
    }

    public static synchronized void registerClient() {
        if (clientRegistered) {
            SparkFactionApiMod.LOGGER.info("SparkFactionAPI client login version receiver is already registered.");
            return;
        }
        clientRegistered = true;

        boolean registered = ClientLoginNetworking.registerGlobalReceiver(VersionProtocol.VERSION_CHECK_ID,
                (client, handler, buf, callbacks) -> {
                    String serverVersion = VersionProtocol.readVersion(buf);
                    String clientVersion = VersionProtocol.localVersion();
                    SparkFactionApiMod.LOGGER.info(
                            "Answering SparkFactionAPI login version query: server={}, client={}.",
                            serverVersion,
                            clientVersion
                    );
                    return CompletableFuture.completedFuture(VersionProtocol.writeVersion(clientVersion));
                });
        if (registered) {
            // The server treats an unregistered receiver as a missing client-side mod.
            // 服务端会把未注册的接收器判定为客户端缺少该模组。
            SparkFactionApiMod.LOGGER.info(
                    "Registered SparkFactionAPI client login version receiver on channel {}.",
                    VersionProtocol.VERSION_CHECK_ID
            );
        } else {
            SparkFactionApiMod.LOGGER.warn(
                    "SparkFactionAPI client login version receiver already existed on channel {}.",
                    VersionProtocol.VERSION_CHECK_ID
            );
        }
    }
}
