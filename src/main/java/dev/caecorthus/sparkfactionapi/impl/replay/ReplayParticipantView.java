package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.ReplayPlayerView;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Snapshot handed to tooltip contributors while the replay session is built.
 * 构建回放会话时交给悬停提示贡献者的快照。
 */
record ReplayParticipantView(UUID uuid, ServerWorld world) implements ReplayPlayerView {
}
