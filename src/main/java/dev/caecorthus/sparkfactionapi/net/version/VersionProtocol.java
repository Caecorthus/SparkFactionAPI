package dev.caecorthus.sparkfactionapi.net.version;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class VersionProtocol {
    public static final Identifier VERSION_CHECK_ID = SparkFactionApiMod.id("version_check");

    private static final String MOD_NAME = "SparkFactionAPI";
    /**
     * Longest version string accepted from a login response. The value is client-supplied and reaches the server log
     * and the disconnect text, so it is not read at the default 32767-character limit. Writing is unchanged.
     * 登录响应中接受的最长版本字符串。该值由客户端提供，会进入服务端日志与断开提示，
     * 因此不按默认的 32767 字符上限读取。写入不变。
     */
    public static final int MAX_VERSION_LENGTH = 128;

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
        return buf.readString(MAX_VERSION_LENGTH);
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
