package dev.caecorthus.sparkfactionapi.net.replay;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Marks player-name spans inside snapshot lines through {@code Style#insertion}, so the replay screen can show the
 * roster tooltip for the hovered name without each line carrying its own hover payload.
 * 通过 {@code Style#insertion} 标记快照行中的玩家名片段，使回放界面能为悬停的名字显示名单提示，而无需每行自带悬停内容。
 */
public final class ReplayTextTags {
    private static final String PREFIX = "sparkfactionapi:replay_player/";

    private ReplayTextTags() {
    }

    public static String tag(UUID uuid) {
        return PREFIX + uuid;
    }

    /** The tagged player, or null for any other insertion. 被标记的玩家；其他插入内容返回 null。 */
    public static @Nullable UUID parse(@Nullable String insertion) {
        if (insertion == null || !insertion.startsWith(PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(insertion.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
