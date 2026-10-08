package dev.caecorthus.sparkfactionapi.client.record;

import dev.caecorthus.sparkfactionapi.net.record.MatchRecordPayload;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;

/**
 * Client receiver for the match record: keeps only the latest snapshot and forgets it on disconnect. It never opens
 * a screen or touches the replay flow. Read it through {@code SparkMatchRecordClient}.
 * 对局记录的客户端接收：只保留最新快照，断开连接时清除。不会打开界面，也不影响回放流程。请通过 {@code SparkMatchRecordClient} 读取。
 */
public final class MatchRecordClient {
    private static volatile @Nullable MatchRecordSnapshot latest;

    private MatchRecordClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MatchRecordPayload.ID,
                (payload, context) -> latest = payload.snapshot());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> latest = null);
    }

    public static @Nullable MatchRecordSnapshot latest() {
        return latest;
    }
}
