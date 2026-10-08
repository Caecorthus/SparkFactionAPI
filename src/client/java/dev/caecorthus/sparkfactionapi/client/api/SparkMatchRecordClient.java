package dev.caecorthus.sparkfactionapi.client.api;

import dev.caecorthus.sparkfactionapi.client.record.MatchRecordClient;
import dev.caecorthus.sparkfactionapi.net.record.MatchRecordSnapshot;
import org.jetbrains.annotations.Nullable;

/**
 * Client read access to the record of the last match that ended on the current server. The server sends it once per
 * match, before the round turns INACTIVE on the client, so it is already here when the round-end screen appears.
 * It is null before the first finished match of a connection and is cleared on disconnect.
 * <p>
 * Stable cross-mod contract: SparkAssist reads this class reflectively. Keep the fully qualified class name
 * {@code dev.caecorthus.sparkfactionapi.client.api.SparkMatchRecordClient}, the method {@code latest()}, and the
 * {@link MatchRecordSnapshot} accessors {@code matchId()}, {@code events()}, {@code seq()}, {@code type()},
 * {@code tick()} and {@code data()}.
 * <p>
 * 客户端读取当前服务器上最近一局已结束对局记录的入口。服务端每局发送一次，且早于客户端上回合变为 INACTIVE，
 * 因此出现局末画面时记录已经到达。本次连接中尚无已结束对局时为 null，断开连接时清除。
 * <p>
 * 稳定的跨模组契约：SparkAssist 通过反射读取本类。请保持完整类名
 * {@code dev.caecorthus.sparkfactionapi.client.api.SparkMatchRecordClient}、方法 {@code latest()}，以及
 * {@link MatchRecordSnapshot} 的访问器 {@code matchId()}、{@code events()}、{@code seq()}、{@code type()}、
 * {@code tick()}、{@code data()} 不变。
 */
public final class SparkMatchRecordClient {
    private SparkMatchRecordClient() {
    }

    /**
     * The latest received match record, or null. Call it on the client thread; treat event NBT as read-only.
     * 最近收到的对局记录，没有时为 null。请在客户端线程调用；事件 NBT 请视为只读。
     */
    public static @Nullable MatchRecordSnapshot latest() {
        return MatchRecordClient.latest();
    }
}
