package dev.caecorthus.sparkfactionapi.net.version;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;

public final class ServerVersionHandshake {
    private static boolean serverRegistered;

    private ServerVersionHandshake() {
    }

    public static synchronized void registerServer() {
        if (serverRegistered) {
            return;
        }
        serverRegistered = true;
        SparkFactionApiMod.LOGGER.info(
                "Registering SparkFactionAPI login version check on channel {}.",
                VersionProtocol.VERSION_CHECK_ID
        );

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            String serverVersion = VersionProtocol.localVersion();
            SparkFactionApiMod.LOGGER.info(
                    "Sending SparkFactionAPI login version query {} to {} with server version {}.",
                    VersionProtocol.VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            ServerLoginNetworking.registerReceiver(handler, VersionProtocol.VERSION_CHECK_ID,
                    (minecraftServer, networkHandler, understood, buf, loginSynchronizer, responseSender) ->
                            handleResponse(networkHandler, understood, buf, serverVersion));
            sender.sendPacket(VersionProtocol.VERSION_CHECK_ID, VersionProtocol.writeVersion(serverVersion));
        });
    }

    private static void handleResponse(
            ServerLoginNetworkHandler handler,
            boolean understood,
            PacketByteBuf buf,
            String serverVersion
    ) {
        // Reject answered mismatches early, but tolerate unanswered login queries behind proxies.
        // 已回应但版本不一致时尽早拒绝；代理后的未回应登录查询则允许继续。
        if (!understood) {
            SparkFactionApiMod.LOGGER.warn(
                    "SparkFactionAPI login version query {} was not understood by {}. Expected client version {}.",
                    VersionProtocol.VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            if (VersionProtocol.shouldRejectUnansweredLoginQuery()) {
                handler.disconnect(Text.literal(VersionProtocol.missingClientMessage(serverVersion)));
            } else {
                SparkFactionApiMod.LOGGER.warn(
                        "Allowing {} to continue because proxies can drop Fabric login-query responses.",
                        handler.getConnectionInfo()
                );
            }
            return;
        }

        String clientVersion = VersionProtocol.readVersion(buf);
        SparkFactionApiMod.LOGGER.info(
                "Received SparkFactionAPI login version response from {}: client={}, server={}.",
                handler.getConnectionInfo(),
                clientVersion,
                serverVersion
        );
        if (!VersionProtocol.isCompatible(serverVersion, clientVersion)) {
            SparkFactionApiMod.LOGGER.warn(
                    "Rejecting SparkFactionAPI version mismatch for {}: client={}, server={}.",
                    handler.getConnectionInfo(),
                    clientVersion,
                    serverVersion
            );
            handler.disconnect(Text.literal(VersionProtocol.mismatchMessage(serverVersion, clientVersion)));
        }
    }
}
