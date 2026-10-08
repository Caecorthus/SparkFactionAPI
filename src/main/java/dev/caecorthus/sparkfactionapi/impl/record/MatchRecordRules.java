package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.net.record.MatchRecordSnapshot;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Pure selection rules for the match record payload. Every cut keeps the earliest events, so the opening
 * {@code role_assigned} snapshot always survives and only the tail of an oversized match is lost.
 * 对局记录数据包的纯选择规则。所有截断都保留最早的事件，因此开局 {@code role_assigned} 快照总能保留，超大对局只丢失末尾部分。
 */
public final class MatchRecordRules {
    private MatchRecordRules() {
    }

    /**
     * @param cut sendable events dropped by the event cap; 因事件数上限而丢弃的可发送事件数
     */
    public record Selection(List<MatchRecordSnapshot.Event> events, int cut) {
        public Selection {
            events = List.copyOf(events);
        }
    }

    /**
     * Skips {@code door_interaction} (positional noise) and types too long to encode, makes ticks relative to the
     * match start (clamped to 0..Integer.MAX_VALUE), copies each event's NBT, and keeps the earliest {@code maxEvents}.
     * 跳过 {@code door_interaction}（位置噪声）与过长无法编码的类型，把 tick 换算为相对对局开始（限制在 0..Integer.MAX_VALUE），
     * 复制每个事件的 NBT，并保留最早的 {@code maxEvents} 条。
     */
    public static Selection select(List<GameRecordEvent> recorded, long startTick, int maxEvents) {
        List<MatchRecordSnapshot.Event> kept = new ArrayList<>(Math.min(recorded.size(), Math.max(maxEvents, 0)));
        int cut = 0;
        for (GameRecordEvent event : recorded) {
            if (!isSent(event.type())) {
                continue;
            }
            if (kept.size() >= maxEvents) {
                cut++;
                continue;
            }
            // Copy: on an integrated server the payload object reaches the client without encoding.
            // 复制：单人内置服务端中数据包对象不经编码直接交给客户端。
            NbtCompound data = event.data() == null ? new NbtCompound() : event.data().copy();
            kept.add(new MatchRecordSnapshot.Event(
                    event.seq(),
                    event.type(),
                    relativeTick(event.worldTick(), startTick),
                    data
            ));
        }
        return new Selection(kept, cut);
    }

    static boolean isSent(@Nullable String type) {
        return type != null
                && type.length() <= MatchRecordSnapshot.MAX_TYPE_LENGTH
                && !GameRecordTypes.DOOR_INTERACTION.equals(type);
    }

    public static int relativeTick(long worldTick, long startTick) {
        return Math.clamp(worldTick - startTick, 0, Integer.MAX_VALUE);
    }

    /**
     * Length of the longest prefix whose summed {@code encodedSize} stays within {@code budget}.
     * 返回累计 {@code encodedSize} 不超过 {@code budget} 的最长前缀长度。
     */
    public static int fittingPrefix(
            List<MatchRecordSnapshot.Event> events,
            ToLongFunction<MatchRecordSnapshot.Event> encodedSize,
            long budget
    ) {
        long used = 0;
        for (int i = 0; i < events.size(); i++) {
            used += encodedSize.applyAsLong(events.get(i));
            if (used > budget) {
                return i;
            }
        }
        return events.size();
    }
}
