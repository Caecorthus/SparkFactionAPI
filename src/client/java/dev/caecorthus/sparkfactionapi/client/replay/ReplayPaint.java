package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Language;

import java.util.List;

/**
 * Procedural drawing primitives for the replay screen: Harpy Express panel recipe, pixel icons, slim scrollbars and
 * a brass tooltip. Integer geometry only; {@code fill(x1, y1, x2, y2)} has exclusive x2/y2.
 * 回放界面的程序化绘制原语：哈比特快面板配方、像素图标、细滚动条与黄铜提示框。只用整数坐标；{@code fill} 的 x2/y2 为开区间。
 */
final class ReplayPaint {
    static final String ELLIPSIS = "…";
    static final int ICON = 7;
    static final int SCROLLBAR_GUTTER = 5;
    static final int FADE = 6;

    private static final String[] HISTORY = {"#.#####..", "##.....#.", "###.#...#", "....#...#", "#...###.#",
            "#.......#", ".#.....#.", "..#####.."};
    private static final String[] SKULL = {".#####.", "#######", "#..#..#", "#..#..#", "###.###", ".#####.", ".#.#.#."};
    private static final String[] SWAP = {"....#..", "#######", "....#..", ".......", "..#....", "#######", "..#...."};
    private static final String[] SPARK = {"...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."};
    private static final String[] COIN_ICON = {"..###..", ".#...#.", "#..#..#", "#..#..#", "#..#..#", ".#...#.",
            "..###.."};
    private static final String[] CHEST = {".#####.", "#.....#", "#######", "#..#..#", "#..#..#", "#..#..#", "#######"};
    private static final String[] BULLET = {".#.", "###", ".#."};
    private static final String[] CHECK = {"......#", ".....##", "#...##.", "##.##..", ".###...", "..#...."};
    private static final String[] STAR = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", ".#...#."};
    private static final String[] EXIT = {"####...", "#......", "#...#..", "#.#####", "#...#..", "#......", "####..."};
    private static final String[] CLOSE = {"#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##", "#.....#"};
    private static final String[] MAGNIFIER = {".####....", "#....#...", "#....#...", "#....#...", "#....#...",
            ".######..", ".....###.", "......###", ".......##"};
    private static final String[] CLEAR = {"#...#", ".#.#.", "..#..", ".#.#.", "#...#"};

    private ReplayPaint() {
    }

    // ================================================================ frame / 边框

    static void fill(DrawContext c, ReplayRect r, int color) {
        c.fill(r.x(), r.y(), r.right(), r.bottom(), color);
    }

    static void roundedFill(DrawContext c, int x, int y, int w, int h, int color) {
        if (w < 3 || h < 3) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + 1, y, x + w - 1, y + 1, color);
        c.fill(x, y + 1, x + w, y + h - 1, color);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
    }

    static void roundedOutline(DrawContext c, int x, int y, int w, int h, int color) {
        if (w < 3 || h < 3) {
            return;
        }
        c.fill(x + 1, y, x + w - 1, y + 1, color);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /**
     * Mahogany + brass panel (drop shadow, cut corners, walnut bevel, brass ring); content starts 3 px inside.
     * 桃花心木黄铜面板（投影、切角、胡桃木斜面、黄铜环）；内容区内缩 3 像素。
     */
    static void panel(DrawContext c, ReplayRect r) {
        int x = r.x();
        int y = r.y();
        int w = r.width();
        int h = r.height();
        if (w < 8 || h < 8) {
            fill(c, r, PANEL);
            return;
        }
        c.fill(x + w, y + 2, x + w + 1, y + h, SHADOW);
        c.fill(x + 2, y + h, x + w, y + h + 1, SHADOW);
        roundedOutline(c, x, y, w, h, EDGE);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        // Walnut bevel: lit top/left, shaded bottom/right. 胡桃木斜面：左上亮、右下暗。
        c.fill(x + 1, y + 1, x + w - 1, y + 2, RIM_HI);
        c.fill(x + 1, y + 2, x + 2, y + h - 1, RIM_HI);
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, RIM_LO);
        c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, RIM_LO);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, RIM_MITER);
        c.fill(x + 1, y + h - 2, x + 2, y + h - 1, RIM_MITER);
        // Brass ring at inset 2, fading top to bottom. 内缩 2 像素的黄铜环，自上而下渐暗。
        c.fill(x + 2, y + 2, x + w - 2, y + 3, BRASS_HI);
        c.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, BRASS_LO);
        c.fillGradient(x + 2, y + 3, x + 3, y + h - 3, BRASS_HI, BRASS_LO);
        c.fillGradient(x + w - 3, y + 3, x + w - 2, y + h - 3, BRASS_HI, BRASS_LO);
    }

    /** Header band from the panel ring down to {@code bottom}, closed by an engraved brass rule. 标题带及黄铜刻线。 */
    static void band(DrawContext c, ReplayRect panel, int bottom) {
        int x1 = panel.x() + 3;
        int x2 = panel.right() - 3;
        c.fill(x1, panel.y() + 3, x2, bottom, BAND);
        c.fill(x1, bottom, x2, bottom + 1, BRASS_LO);
        c.fill(x1, bottom + 1, x2, bottom + 2, EDGE);
    }

    static void etched(DrawContext c, int x1, int x2, int y) {
        c.fill(x1, y, x2, y + 1, EDGE);
        c.fill(x1, y + 1, x2, y + 2, ETCH_LIGHT);
    }

    static void etchedVertical(DrawContext c, int x, int y1, int y2) {
        c.fill(x, y1, x + 1, y2, EDGE);
        c.fill(x + 1, y1, x + 2, y2, ETCH_LIGHT);
    }

    /** Recessed well: dark fill, 1 px shade on top, lip at the bottom. 凹槽：深色填充、顶部阴影、底部唇边。 */
    static void well(DrawContext c, ReplayRect r) {
        if (r.isEmpty()) {
            return;
        }
        fill(c, r, WELL);
        c.fill(r.x(), r.y(), r.right(), r.y() + 1, EDGE);
        c.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), WELL_LIP);
    }

    // ================================================================ scroll / 滚动

    /** Brass rod in a 3 px gutter; {@code hot} = hovered or dragged. 3 像素槽中的黄铜滑杆。 */
    static void scrollbar(DrawContext c, int gx, int vy, int vh, int ty, int th, boolean hot) {
        c.fill(gx + 1, vy, gx + 2, vy + vh, EDGE);
        int body = hot ? BRASS : BRASS_LO;
        int light = hot ? BRASS_HI : BRASS;
        int shade = hot ? BRASS_LO : THUMB_SHADE;
        c.fill(gx, ty, gx + 3, ty + th, body);
        c.fill(gx, ty, gx + 1, ty + th, light);
        c.fill(gx, ty, gx + 3, ty + 1, light);
        c.fill(gx + 2, ty + 1, gx + 3, ty + th, shade);
        c.fill(gx + 1, ty + th - 1, gx + 3, ty + th, shade);
    }

    /**
     * Soft edges on a scrolled viewport, drawn at local z+1 so they also cover glyphs (+0.03) while staying under the
     * z=400 tooltip. {@code rgb} is the colour behind the list.
     * 滚动视口的柔和边缘，位于局部 z+1，能盖住文字（+0.03）但仍低于 z=400 的提示框。{@code rgb} 为列表背后的颜色。
     */
    static void scrollFades(DrawContext c, ReplayRect content, ReplayScroll scroll, int rgb) {
        if (!scroll.scrollable() || content.height() < 24) {
            return;
        }
        int solid = 0xFF000000 | rgb;
        int clear = rgb & 0xFFFFFF;
        if (scroll.offset() > 0) {
            c.fillGradient(content.x(), content.y(), content.right(), content.y() + FADE, 1, solid, clear);
        }
        if (scroll.offset() < scroll.max()) {
            c.fillGradient(content.x(), content.bottom() - FADE, content.right(), content.bottom(), 1, clear, solid);
        }
    }

    // ================================================================ icons / 图标

    static void history(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, HISTORY);
    }

    static void skull(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, SKULL);
    }

    static void check(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, CHECK);
    }

    static void star(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, STAR);
    }

    static void exit(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, EXIT);
    }

    static void close(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, CLOSE);
    }

    static void magnifier(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, MAGNIFIER);
    }

    static void clear(DrawContext c, int x, int y, int color) {
        bitmap(c, x, y, color, CLEAR);
    }

    /** 7x7 category glyph. 7x7 分类图标。 */
    static void category(DrawContext c, ReplaySnapshot.Category category, int x, int y, int color) {
        switch (category) {
            case DEATH -> bitmap(c, x, y, color, SKULL);
            case CONVERSION -> bitmap(c, x, y, color, SWAP);
            case SKILL -> bitmap(c, x, y, color, SPARK);
            case SHOP -> bitmap(c, x, y, color, COIN_ICON);
            case ITEM -> bitmap(c, x, y, color, CHEST);
            case OTHER -> bitmap(c, x + 2, y + 2, color, BULLET);
        }
    }

    /** '#' = 1 px; horizontal runs merge into one fill. '#' 表示 1 像素；同一行连续像素合并为一次填充。 */
    static void bitmap(DrawContext c, int x, int y, int color, String... rows) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int start = line.indexOf('#');
            while (start >= 0) {
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == '#') {
                    end++;
                }
                c.fill(x + start, y + row, x + end, y + row + 1, color);
                start = line.indexOf('#', end);
            }
        }
    }

    // ================================================================ tooltip / 提示框

    /**
     * Brass tooltip near the pointer, kept on screen like vanilla's hovered positioner, drawn at z+400. Lines keep their
     * own colours; unstyled text is {@code TEXT}.
     * 指针旁的黄铜提示框，像原版一样保持在屏幕内，以 z+400 绘制。各行保留自身颜色，无样式文字为 TEXT。
     */
    static void tooltip(DrawContext c, TextRenderer f, List<OrderedText> lines, int mouseX, int mouseY,
                        int screenW, int screenH) {
        if (lines.isEmpty()) {
            return;
        }
        int w = 0;
        for (OrderedText line : lines) {
            w = Math.max(w, f.getWidth(line));
        }
        int h = lines.size() == 1 ? 8 : 10 * lines.size();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w + 4 > screenW) {
            x = Math.max(4, mouseX - 16 - w);
        }
        if (y + h + 4 > screenH) {
            y = screenH - h - 4;
        }
        y = Math.max(4, y);
        c.getMatrices().push();
        c.getMatrices().translate(0, 0, 400);
        c.fill(x - 3, y - 4, x + w + 3, y - 3, TIP_BG);
        c.fill(x - 3, y + h + 3, x + w + 3, y + h + 4, TIP_BG);
        c.fill(x - 3, y - 3, x + w + 3, y + h + 3, TIP_BG);
        c.fill(x - 4, y - 3, x - 3, y + h + 3, TIP_BG);
        c.fill(x + w + 3, y - 3, x + w + 4, y + h + 3, TIP_BG);
        c.fillGradient(x - 3, y - 2, x - 2, y + h + 2, BRASS_HI, BRASS_LO);
        c.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, BRASS_HI, BRASS_LO);
        c.fill(x - 3, y - 3, x + w + 3, y - 2, BRASS_HI);
        c.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, BRASS_LO);
        int lineY = y;
        for (int i = 0; i < lines.size(); i++) {
            c.drawText(f, lines.get(i), x, lineY, i == 0 ? TEXT_HI : TIP_DESC, true);
            lineY += i == 0 ? 12 : 10;
        }
        c.getMatrices().pop();
    }

    // ================================================================ text / 文字

    /** Trim to {@code maxWidth} with a trailing "…" (its width counts). 超宽时截断并加省略号（省略号宽度计入）。 */
    static String ellipsize(TextRenderer f, String text, int maxWidth) {
        if (f.getWidth(text) <= maxWidth) {
            return text;
        }
        int room = maxWidth - f.getWidth(ELLIPSIS);
        if (room <= 0) {
            return maxWidth >= f.getWidth(ELLIPSIS) ? ELLIPSIS : "";
        }
        return f.trimToWidth(text, room).stripTrailing() + ELLIPSIS;
    }

    /** Styled variant; keeps every span's style. 带样式版本，保留各片段样式。 */
    static OrderedText ellipsize(TextRenderer f, StringVisitable text, int maxWidth) {
        if (f.getWidth(text) <= maxWidth) {
            return Language.getInstance().reorder(text);
        }
        int room = maxWidth - f.getWidth(ELLIPSIS);
        if (room <= 0) {
            return maxWidth >= f.getWidth(ELLIPSIS)
                    ? Language.getInstance().reorder(StringVisitable.plain(ELLIPSIS))
                    : OrderedText.EMPTY;
        }
        StringVisitable kept = f.trimToWidth(text, room);
        return Language.getInstance().reorder(StringVisitable.concat(kept, StringVisitable.plain(ELLIPSIS)));
    }
}
