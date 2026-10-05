package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import dev.caecorthus.sparkfactionapi.net.replay.ReplayTextTags;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Scrollable list of the visible timeline lines: a gray time column, a category glyph and the line text wrapped to
 * the column width. Wrapped lines are cached per snapshot line for the current width; entry offsets are rebuilt only
 * when the filter or width changes, and drawing touches only the entries inside the viewport.
 * 可滚动的时间线可见行列表：灰色时间列、分类图标与按栏宽换行的行文本。换行结果按当前宽度逐行缓存；条目偏移只在筛选或宽度
 * 变化时重建，绘制时只处理视口内的条目。
 */
final class ReplayTimelineView {
    static final int LINE_HEIGHT = 10;
    private static final int PAD_TOP = 2;
    private static final int PAD_EXTRA = 3;
    private static final int PAD_X = 4;
    private static final long FLASH_MS = 1400;

    private final TextRenderer f;
    private final ReplayTimelineModel model;
    private final StringVisitable[] display;
    private final String[] clocks;
    private final int timeWidth;
    private final String emptyLine;
    private final ReplayScroll scroll = new ReplayScroll();
    private List<OrderedText>[] wrapped;
    private int wrapWidth = -1;
    private int[] entryTop = new int[1];
    private int syncedVersion = -1;
    private ReplayRect viewport = ReplayRect.EMPTY;
    private int timeX;
    private int iconX;
    private int textX;
    private int textWidth;
    private int flashLine = -1;
    private long flashStart;

    @SuppressWarnings("unchecked")
    ReplayTimelineView(TextRenderer textRenderer, ReplayTimelineModel model) {
        this.f = textRenderer;
        this.model = model;
        int total = model.total();
        this.display = new StringVisitable[total];
        this.clocks = new String[total];
        int widest = textRenderer.getWidth("00:00");
        for (int i = 0; i < total; i++) {
            ReplaySnapshot.Line line = model.line(i);
            display[i] = displayText(line);
            clocks[i] = ReplayTimeFormat.clock(line.second());
            if (clocks[i].length() > 5) {
                widest = Math.max(widest, textRenderer.getWidth(clocks[i]));
            }
        }
        this.timeWidth = widest;
        this.emptyLine = Text.translatable(ReplayTexts.EMPTY).getString();
        this.wrapped = new List[total];
    }

    /** Line text without a leading glyph the screen already draws in its own column. 去掉界面已在图标列绘制的开头字符。 */
    static StringVisitable displayText(ReplaySnapshot.Line line) {
        return switch (line.category()) {
            case CONVERSION -> ReplayTexts.withoutLeadingGlyph(line.text(), "⇄");
            case DEATH -> ReplayTexts.withoutLeadingGlyph(line.text(), "☠");
            default -> line.text();
        };
    }

    ReplayScroll scroll() {
        return scroll;
    }

    StringVisitable display(int lineIndex) {
        return display[lineIndex];
    }

    // ================================================================ layout / 布局

    void layout(ReplayRect viewport) {
        this.viewport = viewport;
        timeX = viewport.x() + PAD_X;
        iconX = timeX + timeWidth + 4;
        textX = iconX + ReplayPaint.ICON + 4;
        int nextWidth = Math.max(20, viewport.right() - ReplayPaint.SCROLLBAR_GUTTER - 3 - textX);
        if (nextWidth != wrapWidth) {
            wrapWidth = nextWidth;
            Arrays.fill(wrapped, null);
        }
        rebuild();
    }

    /** Rebuilds entry offsets after a filter change, resetting the scroll to the top. 筛选变化后重建条目偏移并回到顶部。 */
    void sync() {
        if (syncedVersion != model.version()) {
            rebuild();
        }
    }

    private void rebuild() {
        if (syncedVersion != model.version()) {
            // A new filter result starts at its top; a resize keeps the position. 新筛选结果从顶部开始；缩放保持位置。
            scroll.scrollTo(0);
            flashLine = -1;
        }
        int count = model.visibleCount();
        if (entryTop.length < count + 1) {
            entryTop = new int[count + 1];
        }
        int y = 0;
        for (int i = 0; i < count; i++) {
            entryTop[i] = y;
            y += entryHeight(lines(model.visibleLine(i)).size());
        }
        entryTop[count] = y;
        syncedVersion = model.version();
        scroll.setBounds(y, viewport.height());
    }

    private static int entryHeight(int lineCount) {
        return PAD_EXTRA + Math.max(1, lineCount) * LINE_HEIGHT;
    }

    private List<OrderedText> lines(int lineIndex) {
        List<OrderedText> cached = wrapped[lineIndex];
        if (cached == null) {
            cached = f.wrapLines(display[lineIndex], wrapWidth);
            if (cached.isEmpty()) {
                cached = List.of(OrderedText.EMPTY);
            }
            wrapped[lineIndex] = cached;
        }
        return cached;
    }

    // ================================================================ navigation / 跳转

    /** Scrolls a snapshot line into the middle of the viewport and flashes it. 把某行滚动到视口中部并闪烁提示。 */
    void reveal(int lineIndex) {
        sync();
        int position = model.visiblePosition(lineIndex);
        if (position < 0) {
            return;
        }
        int top = entryTop[position];
        int height = entryTop[position + 1] - top;
        scroll.scrollTo(top - Math.max(0, (viewport.height() - height) / 2));
        flashLine = lineIndex;
        flashStart = Util.getMeasuringTimeMs();
    }

    /**
     * Scrolls the first visible line at or after {@code second} to the top (the last line when none is later).
     * 把时间不早于 {@code second} 的第一条可见行滚动到顶部（没有更晚的行时取最后一行）。
     */
    void revealSecond(int second) {
        sync();
        int count = model.visibleCount();
        if (count == 0) {
            return;
        }
        int position = count - 1;
        for (int i = 0; i < count; i++) {
            if (model.line(model.visibleLine(i)).second() >= second) {
                position = i;
                break;
            }
        }
        scroll.scrollTo(entryTop[position]);
    }

    /** Second of the first / last entry intersecting the viewport, or -1 when empty. 视口内首条 / 末条的秒数，空时为 -1。 */
    int firstVisibleSecond() {
        int count = model.visibleCount();
        if (count == 0) {
            return -1;
        }
        return model.line(model.visibleLine(entryAt(scroll.offset()))).second();
    }

    int lastVisibleSecond() {
        int count = model.visibleCount();
        if (count == 0) {
            return -1;
        }
        int bottom = Math.max(scroll.offset(), scroll.offset() + viewport.height() - 1);
        return model.line(model.visibleLine(entryAt(bottom))).second();
    }

    /** Visible position whose entry contains content y (clamped). 包含内容纵坐标的条目位置（越界时取边界）。 */
    private int entryAt(int contentY) {
        int count = model.visibleCount();
        int found = Arrays.binarySearch(entryTop, 0, count, contentY);
        int position = found >= 0 ? found : -found - 2;
        return Math.max(0, Math.min(count - 1, position));
    }

    // ================================================================ hit testing / 命中判定

    ReplayRect scrollbarHit() {
        if (!scroll.scrollable()) {
            return ReplayRect.EMPTY;
        }
        return new ReplayRect(viewport.right() - ReplayPaint.SCROLLBAR_GUTTER - 1, viewport.y(),
                ReplayPaint.SCROLLBAR_GUTTER + 1, viewport.height());
    }

    int trackTop() {
        return viewport.y() + 2;
    }

    int track() {
        return Math.max(0, viewport.height() - 4);
    }

    /** Style of the text under the pointer, or null. 指针下文字的样式；没有时为 null。 */
    @Nullable Style styleAt(double mouseX, double mouseY) {
        if (!viewport.contains(mouseX, mouseY) || model.visibleCount() == 0 || mouseX < textX) {
            return null;
        }
        int contentY = (int) Math.floor(mouseY) - viewport.y() + scroll.offset();
        if (contentY >= entryTop[model.visibleCount()]) {
            return null;
        }
        int position = entryAt(contentY);
        List<OrderedText> lines = lines(model.visibleLine(position));
        int row = (contentY - entryTop[position] - PAD_TOP) / LINE_HEIGHT;
        if (contentY - entryTop[position] < PAD_TOP || row < 0 || row >= lines.size()) {
            return null;
        }
        return f.getTextHandler().getStyleAt(lines.get(row), (int) Math.floor(mouseX) - textX);
    }

    /** Player whose name span is under the pointer, or null. 指针下名字片段对应的玩家；没有时为 null。 */
    @Nullable UUID nameAt(double mouseX, double mouseY) {
        Style style = styleAt(mouseX, mouseY);
        return style == null ? null : ReplayTextTags.parse(style.getInsertion());
    }

    // ================================================================ render / 绘制

    void render(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled, ReplayHover hover,
                ReplayTooltips tooltips) {
        ReplayPaint.well(c, viewport);
        if (viewport.height() <= 4) {
            return;
        }
        int count = model.visibleCount();
        if (count == 0) {
            String shown = ReplayPaint.ellipsize(f, emptyLine, viewport.width() - 8);
            int x = viewport.x() + (viewport.width() - f.getWidth(shown)) / 2;
            int y = viewport.y() + Math.min(24, Math.max(2, viewport.height() / 2 - 4));
            c.drawText(f, shown, x, y, FAINT, false);
            return;
        }
        boolean mouseInside = hoverEnabled && viewport.contains(mouseX, mouseY);
        int offset = scroll.offset();
        int hoveredPosition = mouseInside ? entryAt(mouseY - viewport.y() + offset) : -1;
        if (hoveredPosition >= 0 && mouseY - viewport.y() + offset >= entryTop[count]) {
            hoveredPosition = -1;
        }
        long now = Util.getMeasuringTimeMs();
        int rowX1 = viewport.x() + 1;
        int rowX2 = viewport.right() - ReplayPaint.SCROLLBAR_GUTTER - 1;

        c.enableScissor(viewport.x(), viewport.y() + 1, viewport.right(), viewport.bottom() - 1);
        for (int position = entryAt(offset); position < count; position++) {
            int top = viewport.y() + entryTop[position] - offset;
            if (top >= viewport.bottom()) {
                break;
            }
            int bottom = viewport.y() + entryTop[position + 1] - offset;
            int lineIndex = model.visibleLine(position);
            ReplaySnapshot.Line line = model.line(lineIndex);
            ReplaySnapshot.Category category = line.category();

            if (category == ReplaySnapshot.Category.CONVERSION) {
                c.fill(rowX1, top, rowX2, bottom, CONVERSION_ROW);
                c.fill(rowX1, top, rowX1 + 2, bottom, CONVERSION);
            } else if (category == ReplaySnapshot.Category.DEATH) {
                c.fill(rowX1, top, rowX2, bottom, DEATH_ROW);
                c.fill(rowX1, top, rowX1 + 2, bottom, DEATH);
            } else if ((position & 1) == 1) {
                c.fill(rowX1, top, rowX2, bottom, ZEBRA);
            }
            if (position == hoveredPosition) {
                c.fill(rowX1, top, rowX2, bottom, HOVER);
            }
            if (lineIndex == flashLine) {
                long age = now - flashStart;
                if (age < FLASH_MS) {
                    int alpha = (int) (0x70 * (1.0 - (double) age / FLASH_MS));
                    c.fill(rowX1, top, rowX2, bottom, withAlpha(COIN, alpha));
                } else {
                    flashLine = -1;
                }
            }

            c.drawText(f, clocks[lineIndex], timeX, top + PAD_TOP, position == hoveredPosition ? MUTED : FAINT, false);
            ReplayPaint.category(c, category, iconX, top + PAD_TOP, ReplayPalette.glyph(category));
            List<OrderedText> lines = lines(lineIndex);
            for (int row = 0; row < lines.size(); row++) {
                c.drawText(f, lines.get(row), textX, top + PAD_TOP + row * LINE_HEIGHT, TEXT, true);
            }
        }
        ReplayPaint.scrollFades(c, new ReplayRect(viewport.x() + 1, viewport.y() + 1, rowX2 - rowX1,
                viewport.height() - 2), scroll, WELL_RGB);
        c.disableScissor();

        if (scroll.scrollable()) {
            boolean hot = scroll.dragging() || scrollbarHit().contains(mouseX, mouseY);
            int track = track();
            ReplayPaint.scrollbar(c, viewport.right() - 4, trackTop(), track, scroll.thumbTop(trackTop(), track),
                    scroll.thumbHeight(track), hot);
        }

        if (mouseInside && hoveredPosition >= 0) {
            Style style = styleAt(mouseX, mouseY);
            if (style != null) {
                UUID uuid = ReplayTextTags.parse(style.getInsertion());
                ReplaySnapshot.Player player = model.player(uuid);
                if (player != null) {
                    hover.lines(tooltips.player(player));
                } else if (style.getHoverEvent() != null) {
                    hover.hoverEvent(style);
                }
            }
        }
    }
}
