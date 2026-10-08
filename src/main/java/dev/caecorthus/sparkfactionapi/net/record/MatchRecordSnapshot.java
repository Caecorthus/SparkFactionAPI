package dev.caecorthus.sparkfactionapi.net.record;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Structured facts of one finished Wathe match: the server's {@code GameRecordManager.MatchRecord} events in record
 * order, without {@code door_interaction} events, capped to the earliest ones that fit the payload. Each event keeps
 * Wathe's NBT unchanged: {@code actor}/{@code target} are NBT UUIDs (a death has actor = killer, target = victim and
 * a {@code death_reason}). This is not display data; the replay screen uses {@code ReplaySnapshot}.
 * <p>
 * Stable cross-mod contract: the record and accessor names {@code matchId()}, {@code events()}, {@code seq()},
 * {@code type()}, {@code tick()} and {@code data()} are read reflectively by SparkAssist; do not rename them.
 * <p>
 * 单局已结束 Wathe 对局的结构化事实：服务端 {@code GameRecordManager.MatchRecord} 的事件，按记录顺序排列，不含
 * {@code door_interaction} 事件，并截取放得进数据包的最早部分。每个事件原样保留 Wathe 的 NBT：{@code actor}/{@code target}
 * 为 NBT UUID（死亡事件中 actor 为击杀者、target 为受害者，并带 {@code death_reason}）。这不是展示数据；回放界面使用
 * {@code ReplaySnapshot}。
 * <p>
 * 稳定的跨模组契约：记录及访问器名 {@code matchId()}、{@code events()}、{@code seq()}、{@code type()}、{@code tick()}、
 * {@code data()} 由 SparkAssist 通过反射读取，请勿改名。
 */
public record MatchRecordSnapshot(UUID matchId, List<Event> events) {
    /** Most events one snapshot carries. 单个快照最多携带的事件数。 */
    public static final int MAX_EVENTS = 8192;
    /** Longest event type that can be encoded. 可编码的事件类型最大长度。 */
    public static final int MAX_TYPE_LENGTH = 256;

    public static final PacketCodec<ByteBuf, MatchRecordSnapshot> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, MatchRecordSnapshot::matchId,
            Event.CODEC.collect(PacketCodecs.toList(MAX_EVENTS)), MatchRecordSnapshot::events,
            MatchRecordSnapshot::new
    );

    public MatchRecordSnapshot {
        Objects.requireNonNull(matchId, "matchId");
        events = List.copyOf(events);
    }

    /**
     * One recorded event. {@code seq} is Wathe's sequence number; {@code tick} counts world ticks since the match
     * started (never negative). Treat {@code data} as read-only.
     * 一条记录事件。{@code seq} 为 Wathe 序号；{@code tick} 为对局开始后经过的世界 tick（不为负）。{@code data} 请视为只读。
     */
    public record Event(int seq, String type, int tick, NbtCompound data) {
        public static final PacketCodec<ByteBuf, Event> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Event::seq,
                PacketCodecs.string(MAX_TYPE_LENGTH), Event::type,
                PacketCodecs.VAR_INT, Event::tick,
                PacketCodecs.NBT_COMPOUND, Event::data,
                Event::new
        );

        public Event {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(data, "data");
        }
    }
}
