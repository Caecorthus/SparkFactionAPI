package dev.caecorthus.sparkfactionapi.client.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wrapped tooltip lines, built once per player or badge and width, so hovering never re-wraps per frame.
 * Snapshot tooltips are {@code \n}-separated; vanilla line wrapping already breaks on {@code \n}.
 * 按玩家或词条与宽度缓存的已换行提示，悬停时不会每帧重新换行。快照提示以 {@code \n} 分行，原版换行会在 {@code \n} 处断行。
 */
final class ReplayTooltips {
    static final int MAX_WIDTH = 220;

    private final TextRenderer textRenderer;
    private final Map<UUID, List<OrderedText>> players = new HashMap<>();
    private final Map<ReplaySnapshot.Badge, List<OrderedText>> badges = new IdentityHashMap<>();
    private final Map<String, List<OrderedText>> plain = new HashMap<>();
    private final Map<Text, List<OrderedText>> texts = new IdentityHashMap<>();
    private int width = MAX_WIDTH;

    ReplayTooltips(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    /** Caps the wrap width for small windows; changing it drops every cached entry. 为小窗口限制换行宽度；变化时清空缓存。 */
    void setScreenWidth(int screenWidth) {
        int next = Math.max(60, Math.min(MAX_WIDTH, screenWidth - 24));
        if (next != width) {
            width = next;
            players.clear();
            badges.clear();
            plain.clear();
            texts.clear();
        }
    }

    List<OrderedText> player(ReplaySnapshot.Player player) {
        return players.computeIfAbsent(player.uuid(), uuid -> textRenderer.wrapLines(player.tooltip(), width));
    }

    List<OrderedText> badge(ReplaySnapshot.Badge badge) {
        return badges.computeIfAbsent(badge, b -> textRenderer.wrapLines(
                b.tooltip().orElseGet(() -> b.label().copy().withColor(ReplayPalette.readable(b.color()))), width));
    }

    /** Wrapped text keyed by instance, for header texts that may be cut short. 按实例缓存，用于可能被截断的标题文本。 */
    List<OrderedText> text(Text text) {
        return texts.computeIfAbsent(text, t -> textRenderer.wrapLines(t, width));
    }

    /** Wrapped text keyed by translation key, for fixed UI tooltips. 按翻译键缓存的固定界面提示。 */
    List<OrderedText> key(String translationKey) {
        return plain.computeIfAbsent(translationKey, key -> textRenderer.wrapLines(Text.translatable(key), width));
    }
}
