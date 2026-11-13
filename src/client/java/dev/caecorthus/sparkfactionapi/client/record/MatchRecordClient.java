package dev.caecorthus.sparkfactionapi.client.record;

import dev.caecorthus.sparkfactionapi.net.record.MatchRecordPayload;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;

/**
 * Client receiver for the match record: keeps only the latest snapshot and forgets it on disconnect and when a new
 * play session starts. It never opens
 * a screen or touches the replay flow. Read it through {@code SparkMatchRecordClient}.
 * 对局记录的客户端接收：只保留最新快照，断开连接以及新的 play 会话开始时清除。不会打开界面，也不影响回放流程。请通过 {@code SparkMatchRecordClient} 读取。
 */
public final class MatchRecordClient {
    private static volatile @Nullable MatchRecordSnapshot latest;

    private MatchRecordClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MatchRecordPayload.ID,
                (payload, context) -> latest = payload.snapshot());
        // DISCONNECT may fire on the netty thread (channelInactive), while a record received just before it is still
        // queued for the client thread; clearing through client.execute keeps the clear after that queued write.
        // DISCONNECT 可能在 netty 线程（channelInactive）触发，而断开前刚收到的记录仍在客户端线程队列中；
        // 经 client.execute 清除可保证清除发生在那次排队写入之后。
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> latest = null));
        // A proxy backend switch re-enters the play phase without firing DISCONNECT, so a new session also starts clean.
        // 代理切换后端服时会重新进入 play 阶段而不触发 DISCONNECT，因此新会话开始时同样清除。
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> latest = null);
    }

    public static @Nullable MatchRecordSnapshot latest() {
        return latest;
    }
}
