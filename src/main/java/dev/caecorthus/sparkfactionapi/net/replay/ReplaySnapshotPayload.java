package dev.caecorthus.sparkfactionapi.net.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * S2C: opens the replay screen with this snapshot. Sent only on request (/replay, chat button, key, vote screen).
 * S2C：携带快照并打开回放界面。仅在请求时发送（/replay、聊天按钮、按键、投票界面按钮）。
 */
public record ReplaySnapshotPayload(ReplaySnapshot snapshot) implements CustomPayload {
    public static final Id<ReplaySnapshotPayload> ID = new Id<>(SparkFactionApiMod.id("replay_snapshot"));
    public static final PacketCodec<RegistryByteBuf, ReplaySnapshotPayload> CODEC =
            ReplaySnapshot.CODEC.xmap(ReplaySnapshotPayload::new, ReplaySnapshotPayload::snapshot);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
