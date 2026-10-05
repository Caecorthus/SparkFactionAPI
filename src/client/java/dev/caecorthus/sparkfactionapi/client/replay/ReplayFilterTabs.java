package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Category filter tabs with live counts. Tabs whose category never occurs in the match are hidden ("all" always
 * shows). Each tab reserves room for its whole-match count, so tab widths and rows stay put while filters change.
 * 带实时计数的分类筛选标签。整局从未出现的分类不显示（“全部”始终显示）。每个标签按整局计数预留宽度，筛选变化时标签宽度与行数不变。
 */
final class ReplayFilterTabs {
    private static final int GAP = 2;
    private static final int PAD = 4;
    private static final int DOT = 3;
    private static final int DOT_GAP = 2;

    private final TextRenderer f;
    private final ReplayTimelineModel model;
    private final @Nullable ReplaySnapshot.Category[] categories;
    private final String[] labels;
    private final int[] widths;
    private final int[] labelWidths;
    private int[] xs = new int[0];
    private int[] rows = new int[0];
    private int shown;
    private ReplayRect area = ReplayRect.EMPTY;
    private final String[] countLabels;
    private int countVersion = -1;

    ReplayFilterTabs(TextRenderer textRenderer, ReplayTimelineModel model) {
        this.f = textRenderer;
        this.model = model;
        List<ReplaySnapshot.Category> present = new ArrayList<>();
        present.add(null);
        for (ReplaySnapshot.Category category : ReplaySnapshot.Category.values()) {
            if (model.totalCount(category) > 0) {
                present.add(category);
            }
        }
        this.categories = present.toArray(new ReplaySnapshot.Category[0]);
        this.labels = new String[categories.length];
        this.widths = new int[categories.length];
        this.labelWidths = new int[categories.length];
        this.countLabels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            ReplaySnapshot.Category category = categories[i];
            labels[i] = Text.translatable(ReplayTexts.filterKey(category)).getString();
            labelWidths[i] = f.getWidth(labels[i]);
            int countWidth = f.getWidth(Integer.toString(model.totalCount(category)));
            widths[i] = PAD + (category == null ? 0 : DOT + DOT_GAP) + labelWidths[i] + 4 + countWidth + PAD;
        }
    }

    /** Rows the tabs need at {@code width}. 指定宽度下标签所需行数。 */
    int rowsFor(int width) {
        return Math.max(1, ReplayFlow.layout(widths, width, GAP, 0, 0).rows());
    }

    void layout(ReplayRect area) {
        this.area = area;
        ReplayFlow.Result flow = ReplayFlow.layout(widths, area.width(), GAP, 0, 0);
        xs = flow.x();
        rows = flow.row();
        shown = flow.shown();
    }

    /** Category of the tab under the pointer; returns false when no tab is hit. 指针下的标签。 */
    boolean click(double mouseX, double mouseY) {
        int index = tabAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        ReplaySnapshot.Category category = categories[index];
        // Clicking the active tab again returns to "all". 再次点击当前标签回到“全部”。
        model.setCategory(category != null && category == model.category() ? null : category);
        return true;
    }

    private int tabAt(double mouseX, double mouseY) {
        if (!area.contains(mouseX, mouseY)) {
            return -1;
        }
        for (int i = 0; i < shown; i++) {
            if (rect(i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private ReplayRect rect(int index) {
        int y = area.y() + rows[index] * (ReplayScreenLayout.TAB_ROW_HEIGHT + ReplayScreenLayout.TAB_ROW_GAP);
        int w = Math.min(widths[index], area.width());
        return new ReplayRect(area.x() + xs[index], y, w, Math.min(ReplayScreenLayout.TAB_ROW_HEIGHT, area.bottom() - y));
    }

    void render(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled) {
        if (countVersion != model.version()) {
            for (int i = 0; i < categories.length; i++) {
                countLabels[i] = Integer.toString(model.count(categories[i]));
            }
            countVersion = model.version();
        }
        int hovered = hoverEnabled ? tabAt(mouseX, mouseY) : -1;
        for (int i = 0; i < shown; i++) {
            int x = area.x() + xs[i];
            int y = area.y() + rows[i] * (ReplayScreenLayout.TAB_ROW_HEIGHT + ReplayScreenLayout.TAB_ROW_GAP);
            if (y + ReplayScreenLayout.TAB_ROW_HEIGHT > area.bottom()) {
                continue;
            }
            int w = Math.min(widths[i], area.width());
            int h = ReplayScreenLayout.TAB_ROW_HEIGHT;
            ReplaySnapshot.Category category = categories[i];
            boolean active = category == model.category();
            int count = model.count(category);
            if (active) {
                ReplayPaint.roundedFill(c, x, y, w, h, SELECT);
                ReplayPaint.roundedOutline(c, x, y, w, h, BRASS);
            } else if (i == hovered) {
                ReplayPaint.roundedFill(c, x, y, w, h, HOVER);
                ReplayPaint.roundedOutline(c, x, y, w, h, HOVER_EDGE);
            } else {
                ReplayPaint.roundedOutline(c, x, y, w, h, withAlpha(BRASS_LO, 0x70));
            }
            // Widths reserve the whole-match count; centre the content so a shorter live count leaves even padding.
            // 宽度按整局计数预留；内容居中，实时计数较短时两侧留白均匀。
            String number = countLabels[i];
            int content = (category == null ? 0 : DOT + DOT_GAP) + labelWidths[i] + 4 + f.getWidth(number);
            int tx = x + Math.max(PAD, (w - content) / 2);
            if (category != null) {
                int dot = ReplayPalette.category(category);
                c.fill(tx, y + 4, tx + DOT, y + 4 + DOT, count == 0 && !active ? withAlpha(dot, 0x60) : dot);
                tx += DOT + DOT_GAP;
            }
            int labelColor = active ? TEXT_HI : i == hovered ? TEXT : count == 0 ? withAlpha(FAINT, 0xA0) : MUTED;
            int room = x + w - PAD - tx;
            String label = labelWidths[i] <= room ? labels[i] : ReplayPaint.ellipsize(f, labels[i], room);
            c.drawText(f, label, tx, y + 2, labelColor, false);
            int numberX = tx + f.getWidth(label) + 4;
            if (numberX + f.getWidth(number) <= x + w - 2) {
                c.drawText(f, number, numberX, y + 2, active ? COIN : FAINT, false);
            }
        }
    }
}
