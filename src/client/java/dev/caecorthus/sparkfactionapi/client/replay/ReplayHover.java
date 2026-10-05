package dev.caecorthus.sparkfactionapi.client.replay;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The single tooltip of a frame. Views report what is hovered while they draw; the screen paints it last so it sits
 * above every panel. Either brass lines (roster, badges, markers) or a vanilla hover event (item names in lines).
 * 每帧唯一的提示框。各视图在绘制时报告悬停内容，界面最后绘制，使其位于所有面板之上。内容为黄铜样式的多行文字
 * （名单、词条、标记）或原版悬停事件（行内的物品名等）。
 */
final class ReplayHover {
    private @Nullable List<OrderedText> lines;
    private @Nullable Style style;

    void clear() {
        lines = null;
        style = null;
    }

    void lines(@Nullable List<OrderedText> value) {
        lines = value == null || value.isEmpty() ? null : value;
        style = null;
    }

    void hoverEvent(Style value) {
        style = value;
        lines = null;
    }

    boolean isEmpty() {
        return lines == null && style == null;
    }

    void render(DrawContext c, TextRenderer f, int mouseX, int mouseY, int screenW, int screenH) {
        if (lines != null) {
            ReplayPaint.tooltip(c, f, lines, mouseX, mouseY, screenW, screenH);
        } else if (style != null) {
            c.drawHoverEvent(f, style, mouseX, mouseY);
        }
    }
}
