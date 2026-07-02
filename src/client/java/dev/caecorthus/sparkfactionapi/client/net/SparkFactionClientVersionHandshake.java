package dev.caecorthus.sparkfactionapi.client.net;

import dev.caecorthus.sparkfactionapi.net.SparkFactionVersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;

import java.util.concurrent.CompletableFuture;

public final class SparkFactionClientVersionHandshake {
    private static boolean clientRegistered;

    private SparkFactionClientVersionHandshake() {
    }

    public static synchronized void registerClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        ClientLoginNetworking.registerGlobalReceiver(SparkFactionVersionHandshake.VERSION_CHECK_ID,
                (client, handler, buf, callbacks) -> {
                    SparkFactionVersionHandshake.readVersion(buf);
                    return CompletableFuture.completedFuture(
                            SparkFactionVersionHandshake.writeVersion(SparkFactionVersionHandshake.localVersion()));
                });
    }
}
