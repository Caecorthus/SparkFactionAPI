package dev.caecorthus.sparkfactionapi.net.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * S2C: the finished match's record, sent once to every client that registered it when Wathe ends the match record,
 * before the round-end announcement. Clients only store it; it never opens a screen.
 * S2C：已结束对局的记录。Wathe 结束对局记录时、在局末公告之前，发送一次给所有注册了它的客户端。客户端只保存它，不会打开任何界面。
 */
public record MatchRecordPayload(MatchRecordSnapshot snapshot) implements CustomPayload {
    public static final Id<MatchRecordPayload> ID = new Id<>(SparkFactionApiMod.id("match_record"));
    public static final PacketCodec<ByteBuf, MatchRecordPayload> CODEC =
            MatchRecordSnapshot.CODEC.xmap(MatchRecordPayload::new, MatchRecordPayload::snapshot);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
