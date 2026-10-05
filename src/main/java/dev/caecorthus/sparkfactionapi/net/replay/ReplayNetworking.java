package dev.caecorthus.sparkfactionapi.net.replay;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Common (both sides) payload type registration for the replay screen.
 * 回放界面数据包类型的公共（双端）注册。
 */
public final class ReplayNetworking {
    private ReplayNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(ReplaySnapshotPayload.ID, ReplaySnapshotPayload.CODEC);
    }
}
