package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Language;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom match bar: a track from 0 to the match duration with start / middle / end labels, minute ticks, a band for
 * the time range shown in the list, and markers for the visible deaths (red) and role changes (purple). Lines of the
 * active non-"all" category get faint markers too. Markers follow the timeline filters.
 * 底部对局时间轴：从 0 到对局时长的轨道，带起点 / 中点 / 终点标签、分钟刻度、列表当前显示时间段的色带，以及可见死亡（红）
 * 与身份转化（紫）的标记。选中具体分类时，该分类的行也显示淡色标记。标记跟随时间线筛选。
 */
final class ReplayTimelineBar {
    private static final int HIT_SLOP = 3;
    private static final int CLUSTER = 2;
    private static final int MAX_TIP_LINES = 8;
    private static final int TIP_WIDTH = 280;

    private final TextRenderer f;
    private final ReplayTimelineModel model;
    private final ReplayTimelineView timeline;
    private final int duration;
    private final String durationLabel;
    private final int reportedDuration;
    private int[] markers = new int[0];
    private int markerCount;
    private int syncedVersion = -1;
    private ReplayRect rect = ReplayRect.EMPTY;
    private List<OrderedText> tipLines = List.of();
    private int tipFirst = -1;
    private int tipCount;
    private int tipVersion = -1;

    ReplayTimelineBar(TextRenderer textRenderer, ReplayTimelineModel model, ReplayTimelineView timeline,
                      int durationSeconds) {
        this.f = textRenderer;
        this.model = model;
        this.timeline = timeline;
        int longest = Math.max(0, durationSeconds);
        for (int i = 0; i < model.total(); i++) {
            longest = Math.max(longest, model.line(i).second());
        }
        // Lines past the reported duration stretch the track instead of piling up at the end. 超出时长的行会拉长轨道。
        this.duration = Math.max(1, longest);
        this.reportedDuration = Math.max(0, durationSeconds);
        this.durationLabel = ReplayTimeFormat.clock(reportedDuration);
    }

    void layout(ReplayRect rect) {
        this.rect = rect;
    }

    void sync() {
        if (syncedVersion == model.version()) {
            return;
        }
        int count = model.visibleCount();
        if (markers.length < count) {
            markers = new int[count];
        }
        markerCount = 0;
        for (int i = 0; i < count; i++) {
            int lineIndex = model.visibleLine(i);
            if (marked(model.line(lineIndex).category())) {
                markers[markerCount++] = lineIndex;
            }
        }
        syncedVersion = model.version();
    }

    private boolean marked(ReplaySnapshot.Category category) {
        return category == ReplaySnapshot.Category.DEATH
                || category == ReplaySnapshot.Category.CONVERSION
                || category == model.category();
    }

    // ================================================================ geometry / 几何

    private int trackX1() {
        return rect.x() + 3;
    }

    private int trackX2() {
        return rect.right() - 3;
    }

    private int trackY() {
        return rect.y() + (rect.height() >= 24 ? 5 : 3);
    }

    private int markerX(int second) {
        int span = Math.max(0, trackX2() - trackX1() - 1);
        long clamped = Math.max(0, Math.min(duration, second));
        return trackX1() + (int) (clamped * span / duration);
    }

    /** Whether the pointer is on the track row (markers included). 指针是否位于轨道行（含标记）。 */
    boolean trackHit(double mouseX, double mouseY) {
        return rect.contains(mouseX, mouseY) && mouseY <= trackY() + 9 && mouseX >= trackX1() - 2
                && mouseX < trackX2() + 2;
    }

    /** Match second under the pointer, clamped to the track. 指针处的对局秒数，限制在轨道范围内。 */
    int secondAt(double mouseX) {
        int span = Math.max(1, trackX2() - trackX1() - 1);
        double fraction = Math.max(0, Math.min(1, (mouseX - trackX1()) / span));
        return (int) Math.round(fraction * duration);
    }

    /** Snapshot line of the marker under the pointer (the nearest within a few px), or -1. 指针下最近标记对应的行；没有时为 -1。 */
    int markerAt(double mouseX, double mouseY) {
        if (!rect.contains(mouseX, mouseY) || mouseY > trackY() + 9) {
            return -1;
        }
        int best = -1;
        double bestDistance = HIT_SLOP + 0.5;
        for (int i = 0; i < markerCount; i++) {
            int lineIndex = markers[i];
            double distance = Math.abs(markerX(model.line(lineIndex).second()) + 0.5 - mouseX);
            // Deaths win ties so the red marker drawn on top is the one picked. 距离相同时优先死亡，与绘制顺序一致。
            boolean death = model.line(lineIndex).category() == ReplaySnapshot.Category.DEATH;
            if (distance < bestDistance || (distance == bestDistance && death)) {
                bestDistance = distance;
                best = lineIndex;
            }
        }
        return best;
    }

    // ================================================================ render / 绘制

    void render(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled, ReplayHover hover) {
        if (rect.isEmpty() || rect.width() < 24) {
            return;
        }
        int x1 = trackX1();
        int x2 = trackX2();
        int ty = trackY();

        // Track well and minute ticks. 轨道凹槽与分钟刻度。
        c.fill(x1, ty, x2, ty + 4, WELL);
        c.fill(x1, ty, x2, ty + 1, EDGE);
        c.fill(x1, ty + 3, x2, ty + 4, WELL_LIP);
        int span = Math.max(1, x2 - x1 - 1);
        int step = span * 60L / duration >= 6 ? 60 : span * 300L / duration >= 6 ? 300 : 0;
        if (step > 0) {
            for (int second = step; second < duration; second += step) {
                int x = markerX(second);
                c.fill(x, ty + 4, x + 1, ty + 6, withAlpha(BRASS_LO, 0xA0));
            }
        }

        // Range currently shown in the list. 列表当前显示的时间段。
        int from = timeline.firstVisibleSecond();
        int to = timeline.lastVisibleSecond();
        if (from >= 0 && to >= 0) {
            int bx1 = markerX(Math.min(from, to));
            int bx2 = Math.max(bx1 + 2, markerX(Math.max(from, to)) + 1);
            c.fill(bx1, ty - 1, bx2, ty + 5, withAlpha(BRASS_HI, 0x48));
            c.fill(bx1, ty - 1, bx2, ty, withAlpha(COIN, 0x90));
        }

        int hovered = hoverEnabled ? markerAt(mouseX, mouseY) : -1;
        int hoveredX = hovered >= 0 ? markerX(model.line(hovered).second()) : Integer.MIN_VALUE;
        // Paint order: faint category markers, then role changes, then deaths on top. 绘制顺序：淡色、转化、死亡置顶。
        drawMarkers(c, ty, 0);
        drawMarkers(c, ty, 1);
        drawMarkers(c, ty, 2);
        if (hovered >= 0) {
            int color = ReplayPalette.category(model.line(hovered).category());
            c.fill(hoveredX - 2, ty - 5, hoveredX + 3, ty + 9, TEXT_HI);
            c.fill(hoveredX - 1, ty - 4, hoveredX + 2, ty + 8, color);
        }

        // Labels: start, end, and the middle when there is room. 标签：起点、终点，空间足够时加中点。
        int labelY = ty + 8;
        String start = ReplayTimeFormat.clock(0);
        int startW = f.getWidth(start);
        int endW = f.getWidth(durationLabel);
        c.drawText(f, start, x1, labelY, FAINT, false);
        if (reportedDuration > 0 && x2 - endW > x1 + startW + 4) {
            c.drawText(f, durationLabel, x2 - endW, labelY, FAINT, false);
            String middle = ReplayTimeFormat.clock(duration / 2);
            int midW = f.getWidth(middle);
            int midX = (x1 + x2 - midW) / 2;
            if (duration >= 2 && midX > x1 + startW + 8 && midX + midW < x2 - endW - 8) {
                c.drawText(f, middle, midX, labelY, withAlpha(FAINT, 0xB0), false);
            }
        }

        if (hovered >= 0) {
            hover.lines(tooltip(hovered, hoveredX));
        }
    }

    private void drawMarkers(DrawContext c, int ty, int pass) {
        int lastX = Integer.MIN_VALUE;
        for (int i = 0; i < markerCount; i++) {
            ReplaySnapshot.Category category = model.line(markers[i]).category();
            int markerPass = category == ReplaySnapshot.Category.DEATH ? 2
                    : category == ReplaySnapshot.Category.CONVERSION ? 1 : 0;
            if (markerPass != pass) {
                continue;
            }
            int x = markerX(model.line(markers[i]).second());
            if (x == lastX) {
                continue;
            }
            lastX = x;
            int color = ReplayPalette.category(category);
            if (pass == 0) {
                c.fill(x, ty - 2, x + 1, ty + 6, withAlpha(color, 0xB0));
            } else {
                c.fill(x - 1, ty - 4, x + 2, ty + 8, EDGE);
                c.fill(x, ty - 3, x + 1, ty + 7, color);
                c.fill(x - 1, ty - 3, x, ty + 7, withAlpha(color, 0x90));
                c.fill(x + 1, ty - 3, x + 2, ty + 7, withAlpha(color, 0x90));
            }
        }
    }

    /** Lines of every marker within a couple of px of the hovered one; cached until the hover moves. 悬停标记附近所有行。 */
    private List<OrderedText> tooltip(int hovered, int hoveredX) {
        int first = -1;
        int count = 0;
        for (int i = 0; i < markerCount; i++) {
            if (Math.abs(markerX(model.line(markers[i]).second()) - hoveredX) <= CLUSTER) {
                if (first < 0) {
                    first = i;
                }
                count++;
            }
        }
        if (first == tipFirst && count == tipCount && tipVersion == model.version()) {
            return tipLines;
        }
        List<OrderedText> lines = new ArrayList<>(Math.min(count, MAX_TIP_LINES) + 1);
        Style clockStyle = Style.EMPTY.withColor(TextColor.fromRgb(FAINT & 0xFFFFFF));
        int shown = 0;
        for (int i = first; i < markerCount && shown < MAX_TIP_LINES; i++) {
            int lineIndex = markers[i];
            if (Math.abs(markerX(model.line(lineIndex).second()) - hoveredX) > CLUSTER) {
                continue;
            }
            StringVisitable text = StringVisitable.concat(
                    StringVisitable.styled(ReplayTimeFormat.clock(model.line(lineIndex).second()) + "  ", clockStyle),
                    timeline.display(lineIndex));
            lines.add(ReplayPaint.ellipsize(f, text, TIP_WIDTH));
            shown++;
        }
        if (count > shown) {
            lines.add(Language.getInstance().reorder(StringVisitable.styled("+" + (count - shown), clockStyle)));
        }
        tipFirst = first;
        tipCount = count;
        tipVersion = model.version();
        tipLines = lines.isEmpty() ? List.of() : lines;
        return tipLines;
    }
}
