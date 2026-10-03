package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.ReplayPlayerView;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot handed to tooltip contributors while the replay session is built.
 * 构建回放会话时交给悬停提示贡献者的快照。
 */
record ReplayParticipantView(
        UUID uuid,
        String name,
        ServerWorld world,
        @Nullable ServerPlayerEntity onlinePlayer,
        List<Identifier> roleHistory
) implements ReplayPlayerView {
    ReplayParticipantView {
        roleHistory = List.copyOf(roleHistory);
    }
}
