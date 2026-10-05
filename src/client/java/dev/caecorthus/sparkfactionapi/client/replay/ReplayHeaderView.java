package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Header row: history glyph, bold title and "ended ago" on the first line, "mode · map · duration" below, the outcome
 * chip on the right and the close button in the corner. Texts are resolved and ellipsized on layout.
 * 标题行：历史图标、粗体标题与“多久前结束”在第一行，下方为“模式 · 地图 · 时长”，右侧为结局徽章，角落为关闭按钮。
 * 文本在布局时解析并截断。
 */
final class ReplayHeaderView {
    private static final int GLYPH_W = 9;
    private static final int CHIP_PAD = 5;

    private final TextRenderer f;
    private final Text title;
    private final Text endedAgo;
    private final Text subtitle;
    private final @Nullable Text outcome;
    private final int outcomeColor;
    private final Text fullSubtitle;
    private ReplayRect header = ReplayRect.EMPTY;
    private ReplayRect close = ReplayRect.EMPTY;
    private ReplayRect chip = ReplayRect.EMPTY;
    private OrderedText titleLine = OrderedText.EMPTY;
    private @Nullable String agoLine;
    private int agoX;
    private OrderedText subtitleLine = OrderedText.EMPTY;
    private OrderedText chipLine = OrderedText.EMPTY;
    private int subtitleY;
    private boolean subtitleCut;
    private boolean chipCut;

    ReplayHeaderView(TextRenderer textRenderer, ReplaySnapshot snapshot) {
        this.f = textRenderer;
        this.title = Text.translatable(ReplayTexts.TITLE).formatted(Formatting.BOLD);
        this.endedAgo = Text.translatable(ReplayTexts.ENDED_AGO, ReplayTimeFormat.agoText(snapshot.endedSecondsAgo()));
        this.subtitle = Text.translatable(ReplayTexts.SUBTITLE, ReplayTexts.gameMode(snapshot.gameMode()),
                ReplayTexts.mapEffect(snapshot.mapEffect()), ReplayTimeFormat.clock(snapshot.durationSeconds()));
        this.outcome = snapshot.outcome().orElse(null);
        this.outcomeColor = outcome == null ? COIN : firstColor(outcome, COIN);
        this.fullSubtitle = Text.empty().append(subtitle).append("\n").append(endedAgo);
    }

    /** First explicit colour in the text, else {@code fallback}. 文本中第一个显式颜色；没有时为 fallback。 */
    private static int firstColor(Text text, int fallback) {
        Optional<Integer> found = text.visit((style, part) -> {
            TextColor color = style.getColor();
            return color != null && !part.isEmpty() ? Optional.of(color.getRgb()) : Optional.empty();
        }, Style.EMPTY);
        return found.map(ReplayPalette::opaque).orElse(fallback);
    }

    void layout(ReplayScreenLayout layout) {
        header = layout.header();
        close = layout.close();
        int chipH = layout.compact() ? 12 : 14;
        int right = close.x() - 6;
        if (outcome != null) {
            int maxText = Math.max(0, Math.min(f.getWidth(outcome), header.width() * 2 / 5));
            chipLine = ReplayPaint.ellipsize(f, outcome, maxText);
            chipCut = f.getWidth(chipLine) < f.getWidth(outcome);
            int chipW = CHIP_PAD + ReplayPaint.ICON + 4 + f.getWidth(chipLine) + CHIP_PAD + 1;
            chip = chipW > header.width() / 2 || maxText < 16
                    ? ReplayRect.EMPTY
                    : new ReplayRect(right - chipW, header.y() + (header.height() - chipH) / 2, chipW, chipH);
            if (!chip.isEmpty()) {
                right = chip.x() - 8;
            }
        } else {
            chip = ReplayRect.EMPTY;
        }
        int textX = header.x() + GLYPH_W + 4;
        int room = Math.max(0, right - textX);
        titleLine = ReplayPaint.ellipsize(f, title, room);
        int titleEnd = textX + f.getWidth(titleLine);
        String ago = endedAgo.getString();
        agoX = titleEnd + 8;
        agoLine = f.getWidth(ago) + 2 <= right - agoX ? ago : null;
        subtitleY = header.y() + (layout.compact() ? 12 : 14);
        subtitleLine = ReplayPaint.ellipsize(f, subtitle, Math.max(0, right - textX));
        subtitleCut = agoLine == null || f.getWidth(subtitleLine) < f.getWidth(subtitle);
    }

    boolean closeHit(double mouseX, double mouseY) {
        return close.contains(mouseX, mouseY);
    }

    void render(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled, ReplayHover hover,
                ReplayTooltips tooltips) {
        int x = header.x();
        int y = header.y();
        ReplayPaint.history(c, x, y + 2, BRASS_HI);
        int textX = x + GLYPH_W + 4;
        c.drawText(f, titleLine, textX, y + 2, TITLE, true);
        if (agoLine != null) {
            c.fill(agoX - 5, y + 5, agoX - 3, y + 7, BRASS_LO);
            c.drawText(f, agoLine, agoX, y + 2, FAINT, false);
        }
        c.drawText(f, subtitleLine, textX, subtitleY, MUTED, false);

        if (!chip.isEmpty()) {
            ReplayPaint.roundedFill(c, chip.x(), chip.y(), chip.width(), chip.height(), EDGE);
            ReplayPaint.roundedFill(c, chip.x() + 1, chip.y() + 1, chip.width() - 2, chip.height() - 2,
                    mix(BAND, outcomeColor, 0.22));
            ReplayPaint.roundedOutline(c, chip.x(), chip.y(), chip.width(), chip.height(), mix(outcomeColor, BRASS, 0.35));
            int midY = chip.y() + (chip.height() - 7) / 2;
            ReplayPaint.star(c, chip.x() + CHIP_PAD, midY, outcomeColor);
            c.drawText(f, chipLine, chip.x() + CHIP_PAD + ReplayPaint.ICON + 4, chip.y() + (chip.height() - 8) / 2 + 1,
                    outcomeColor, true);
        }

        boolean hot = hoverEnabled && close.contains(mouseX, mouseY);
        if (!close.isEmpty()) {
            ReplayPaint.roundedFill(c, close.x(), close.y(), close.width(), close.height(), hot ? BUTTON_HOVER : 0x40000000);
            ReplayPaint.roundedOutline(c, close.x(), close.y(), close.width(), close.height(), hot ? COIN : BRASS_LO);
            ReplayPaint.close(c, close.x() + (close.width() - 7) / 2, close.y() + (close.height() - 7) / 2,
                    hot ? TEXT_HI : MUTED);
        }
        if (hot) {
            hover.lines(tooltips.key(ReplayTexts.CLOSE));
        } else if (hoverEnabled && chipCut && chip.contains(mouseX, mouseY)) {
            hover.lines(tooltips.text(outcome));
        } else if (hoverEnabled && subtitleCut && mouseX >= textX && mouseX < header.right() && mouseY >= y
                && mouseY < header.bottom() && !chip.contains(mouseX, mouseY) && !close.contains(mouseX, mouseY)) {
            // Cut-off header texts show in full on hover. 被截断的标题文本在悬停时完整显示。
            hover.lines(tooltips.text(fullSubtitle));
        }
    }
}
