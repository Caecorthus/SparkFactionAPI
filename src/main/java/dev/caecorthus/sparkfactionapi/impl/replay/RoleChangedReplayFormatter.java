package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Replay line for {@code sparkfactionapi:role_changed}: "player: from -> to" plus an optional cause suffix.
 * {@code sparkfactionapi:role_changed} 的回放行：“玩家：原身份 → 新身份”，可附加原因后缀。
 */
public final class RoleChangedReplayFormatter {
    static final String KEY = "replay.sparkfactionapi.role_changed";
    static final String CAUSE_KEY_PREFIX = "replay.sparkfactionapi.role_changed.cause";

    private RoleChangedReplayFormatter() {
    }

    public static @Nullable Text format(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Parsed parsed = parse(event.data());
        if (parsed == null) {
            return null;
        }
        MutableText line = Text.translatable(
                KEY,
                ReplayNameRenderer.plainName(parsed.player()),
                ReplayNameRenderer.roleName(parsed.from()),
                ReplayNameRenderer.roleName(parsed.to())
        ).formatted(Formatting.LIGHT_PURPLE);
        if (parsed.cause() != null) {
            // Empty fallback: a client without this cause key shows nothing extra.
            // 空回退：客户端缺少该原因键时不显示任何附加内容。
            Text sourceName = parsed.source() == null ? Text.empty() : ReplayNameRenderer.nameWithRole(parsed.source());
            line.append(Text.translatableWithFallback(
                    causeTranslationKey(parsed.cause(), parsed.source() != null),
                    "",
                    sourceName
            ));
        }
        return line;
    }

    /** Null when the player or target role is missing or malformed. 玩家或新身份缺失/格式错误时返回 null。 */
    static @Nullable Parsed parse(NbtCompound data) {
        if (!data.containsUuid(ReplayRoleTimeline.KEY_PLAYER)) {
            return null;
        }
        Identifier to = readId(data, ReplayRoleTimeline.KEY_TO);
        if (to == null) {
            return null;
        }
        Identifier from = null;
        if (data.contains(ReplayRoleTimeline.KEY_FROM)) {
            from = readId(data, ReplayRoleTimeline.KEY_FROM);
            if (from == null) {
                return null;
            }
        }
        UUID source = data.containsUuid(ReplayRoleTimeline.KEY_SOURCE) ? data.getUuid(ReplayRoleTimeline.KEY_SOURCE) : null;
        return new Parsed(
                data.getUuid(ReplayRoleTimeline.KEY_PLAYER),
                from,
                to,
                readId(data, ReplayRoleTimeline.KEY_CAUSE),
                source
        );
    }

    /** {@code <prefix>.<ns>.<path>} with '/' as '.', plus {@code .by} when a source exists. 有来源时追加 {@code .by}。 */
    static String causeTranslationKey(Identifier cause, boolean hasSource) {
        String key = CAUSE_KEY_PREFIX + "." + cause.getNamespace() + "." + cause.getPath().replace('/', '.');
        return hasSource ? key + ".by" : key;
    }

    private static @Nullable Identifier readId(NbtCompound data, String key) {
        if (!data.contains(key, NbtElement.STRING_TYPE)) {
            return null;
        }
        String raw = data.getString(key);
        return raw.isEmpty() ? null : Identifier.tryParse(raw);
    }

    record Parsed(UUID player, @Nullable Identifier from, Identifier to, @Nullable Identifier cause, @Nullable UUID source) {
    }
}
