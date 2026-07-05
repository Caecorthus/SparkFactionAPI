package dev.caecorthus.sparkfactionapi.net.version;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class VersionProtocol {
    public static final Identifier VERSION_CHECK_ID = SparkFactionApiMod.id("version_check");

    private static final String MOD_NAME = "SparkFactionAPI";

    private VersionProtocol() {
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

    public static boolean isCompatible(String serverVersion, String clientVersion) {
        return !isBlank(serverVersion) && !isBlank(clientVersion) && serverVersion.equals(clientVersion);
    }

    public static boolean shouldRejectUnansweredLoginQuery() {
        // Proxy transfers can hide Fabric login-query support from a correctly modded client.
        // 代理转服可能让正确安装模组的客户端在登录查询阶段显示为未理解。
        return false;
    }

    public static String missingClientMessage(String serverVersion) {
        return MOD_NAME + " is required on the client with version " + serverVersion + ".";
    }

    public static String mismatchMessage(String serverVersion, String clientVersion) {
        return MOD_NAME + " version mismatch: server=" + serverVersion
                + ", client=" + clientVersion
                + ". Please install the same " + MOD_NAME + " version as the server.";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
