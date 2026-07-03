package dev.caecorthus.sparkfactionapi.net;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SparkFactionVersionHandshake {
    public static final Identifier VERSION_CHECK_ID = SparkFactionApiMod.id("version_check");

    private static boolean serverRegistered;

    private SparkFactionVersionHandshake() {
    }

    public static synchronized void registerServer() {
        if (serverRegistered) {
            return;
        }
        serverRegistered = true;
        SparkFactionApiMod.LOGGER.info(
                "Registering SparkFactionAPI login version check on channel {}.",
                VERSION_CHECK_ID
        );

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            String serverVersion = localVersion();
            SparkFactionApiMod.LOGGER.info(
                    "Sending SparkFactionAPI login version query {} to {} with server version {}.",
                    VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            ServerLoginNetworking.registerReceiver(handler, VERSION_CHECK_ID,
                    (minecraftServer, networkHandler, understood, buf, loginSynchronizer, responseSender) ->
                            handleResponse(networkHandler, understood, buf, serverVersion));
            sender.sendPacket(VERSION_CHECK_ID, writeVersion(serverVersion));
        });
    }

    public static String localVersion() {
        return FabricLoader.getInstance()
                .getModContainer(SparkFactionApiMod.MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();
    }

    public static PacketByteBuf writeVersion(String version) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(version);
        return buf;
    }

    public static String readVersion(PacketByteBuf buf) {
        return buf.readString();
    }

    private static void handleResponse(
            ServerLoginNetworkHandler handler,
            boolean understood,
            PacketByteBuf buf,
            String serverVersion
    ) {
        // Reject before gameplay packets or faction sync can observe mixed jar versions.
        // 在玩法封包或阵营同步前拒绝不一致的 jar 版本。
        if (!understood) {
            SparkFactionApiMod.LOGGER.warn(
                    "SparkFactionAPI login version query {} was not understood by {}. Expected client version {}.",
                    VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            handler.disconnect(Text.literal(SparkFactionVersionCheck.missingClientMessage(serverVersion)));
            return;
        }

        String clientVersion = readVersion(buf);
        SparkFactionApiMod.LOGGER.info(
                "Received SparkFactionAPI login version response from {}: client={}, server={}.",
                handler.getConnectionInfo(),
                clientVersion,
                serverVersion
        );
        if (!SparkFactionVersionCheck.isCompatible(serverVersion, clientVersion)) {
            SparkFactionApiMod.LOGGER.warn(
                    "Rejecting SparkFactionAPI version mismatch for {}: client={}, server={}.",
                    handler.getConnectionInfo(),
                    clientVersion,
                    serverVersion
            );
            handler.disconnect(Text.literal(SparkFactionVersionCheck.mismatchMessage(serverVersion, clientVersion)));
        }
    }
}
