package dev.caecorthus.sparkfactionapi.net.record;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Common (both sides) payload type registration for the match record.
 * 对局记录数据包类型的公共（双端）注册。
 */
public final class MatchRecordNetworking {
    private MatchRecordNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(MatchRecordPayload.ID, MatchRecordPayload.CODEC);
    }
}
