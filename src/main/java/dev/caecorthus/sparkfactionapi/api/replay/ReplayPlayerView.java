package dev.caecorthus.sparkfactionapi.api.replay;

import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Read-only view of one replay participant, captured while the replay is generated.
 * Interface (not record) so later releases can add accessors without breaking contributors.
 * 回放参与者的只读视图，在生成回放时采集。使用接口而非 record，便于后续版本追加访问器而不破坏贡献者。
 */
public interface ReplayPlayerView {
    UUID uuid();

    ServerWorld world();
}
