package dev.caecorthus.sparkfactionapi.api.replay;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of one replay participant, captured while the replay is generated.
 * Interface (not record) so later releases can add accessors without breaking contributors.
 * 回放参与者的只读视图，在生成回放时采集。使用接口而非 record，便于后续版本追加访问器而不破坏贡献者。
 */
public interface ReplayPlayerView {
    UUID uuid();

    /** Best-known account name; never null, may fall back to a UUID prefix. 已知的账号名；不为 null，可能回退为 UUID 前缀。 */
    String name();

    ServerWorld world();

    /** Online player entity, or null when the participant has left. 在线玩家实体；参与者已离开时为 null。 */
    @Nullable ServerPlayerEntity onlinePlayer();

    /**
     * Role ids in chronological order: first is the opening role, last is the final role. Never empty.
     * 按时间顺序排列的身份 id：首个为开局身份，末个为最终身份。不为空。
     */
    List<Identifier> roleHistory();
}
