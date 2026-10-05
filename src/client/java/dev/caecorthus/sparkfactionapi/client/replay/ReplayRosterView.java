package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Roster of player cards: skin head, name in the final role colour, status and result marks, the role chain as chips
 * and up to two rows of badge pills. Card geometry is built on layout, never per frame. One column beside the
 * timeline; a grid when stacked above it.
 * 玩家卡片名单：皮肤头像、按最终身份着色的名字、状态与结局标记、以小标签显示的身份链，以及最多两行词条小标签。
 * 卡片几何只在布局时构建，不会每帧重建。位于时间线旁时为单列，叠放在时间线上方时为网格。
 */
final class ReplayRosterView {
    static final int CARD_GAP = 3;
    static final int GRID_MIN_CARD = 136;
    private static final int INSET = 3;
    private static final int PAD = 4;
    private static final int HEAD = 16;
    private static final int TEXT_X = 25;
    private static final int NAME_Y = 5;
    private static final int ROLE_Y = 14;
    private static final int CHIP_H = 10;
    private static final int BADGE_Y = 27;
    private static final int BADGE_ROW = 11;
    private static final int BADGE_H = 10;
    private static final int MAX_BADGE_ROWS = 2;
    private static final int BASE_HEIGHT = 28;
    private static final String ARROW = "→";

    private final TextRenderer f;
    private final List<ReplaySnapshot.Player> players;
    private final String hint;
    private final ReplayScroll scroll = new ReplayScroll();
    private Card[] cards = new Card[0];
    private ReplayRect viewport = ReplayRect.EMPTY;
    private ReplayRect titleRow = ReplayRect.EMPTY;
    private String titleShown = "";
    private @Nullable String hintShown;
    private int hintX;

    ReplayRosterView(TextRenderer textRenderer, List<ReplaySnapshot.Player> players) {
        this.f = textRenderer;
        this.players = players;
        this.hint = Text.translatable(ReplayTexts.PLAYERS_HINT).getString();
    }

    ReplayScroll scroll() {
        return scroll;
    }

    // ================================================================ layout / 布局

    void layout(ReplayRect viewport, boolean grid) {
        this.viewport = viewport;
        int innerX = viewport.x() + INSET;
        int innerW = Math.max(0, viewport.width() - 2 * INSET - ReplayPaint.SCROLLBAR_GUTTER);
        int columns = grid ? Math.max(1, (innerW + CARD_GAP) / (GRID_MIN_CARD + CARD_GAP)) : 1;
        int cardW = Math.max(0, (innerW - (columns - 1) * CARD_GAP) / columns);
        Card[] built = new Card[players.size()];
        int y = INSET;
        for (int start = 0; start < built.length; start += columns) {
            int rowHeight = 0;
            for (int column = 0; column < columns && start + column < built.length; column++) {
                Card card = new Card(players.get(start + column), innerX + column * (cardW + CARD_GAP), y, cardW,
                        grid);
                built[start + column] = card;
                rowHeight = Math.max(rowHeight, card.height);
            }
            for (int column = 0; column < columns && start + column < built.length; column++) {
                built[start + column].height = rowHeight;
            }
            y += rowHeight + CARD_GAP;
        }
        cards = built;
        int content = built.length == 0 ? 0 : y - CARD_GAP + INSET;
        scroll.setBounds(content, viewport.height());
    }

    /** Scrolls so the player's card is fully visible. 滚动使该玩家卡片完全可见。 */
    void reveal(UUID uuid) {
        for (Card card : cards) {
            if (card.player.uuid().equals(uuid)) {
                if (card.y < scroll.offset()) {
                    scroll.scrollTo(card.y - INSET);
                } else if (card.y + card.height > scroll.offset() + viewport.height()) {
                    scroll.scrollTo(card.y + card.height + INSET - viewport.height());
                }
                return;
            }
        }
    }

    // ================================================================ hit testing / 命中判定

    @Nullable UUID cardAt(double mouseX, double mouseY) {
        Card card = card(mouseX, mouseY);
        return card == null ? null : card.player.uuid();
    }

    private @Nullable Card card(double mouseX, double mouseY) {
        if (!viewport.contains(mouseX, mouseY)) {
            return null;
        }
        double contentY = mouseY - viewport.y() + scroll.offset();
        for (Card card : cards) {
            if (mouseX >= card.x && mouseX < card.x + card.width && contentY >= card.y && contentY < card.y + card.height) {
                return card;
            }
        }
        return null;
    }

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

    // ================================================================ render / 绘制

    /** Ellipsizes "Players (N)" and the hint for the title row; the hint is dropped when narrow. 标题行文本，窄时省略提示。 */
    void layoutTitle(ReplayRect row, String title) {
        titleRow = row;
        titleShown = ReplayPaint.ellipsize(f, title, Math.max(0, row.width() - 2));
        hintX = row.x() + 1 + f.getWidth(titleShown) + 6;
        int room = row.right() - hintX;
        hintShown = room >= 40 ? ReplayPaint.ellipsize(f, hint, room) : null;
    }

    void renderTitle(DrawContext c) {
        if (titleRow.isEmpty()) {
            return;
        }
        c.drawText(f, titleShown, titleRow.x() + 1, titleRow.y() + 2, HEADING, false);
        if (hintShown != null) {
            c.drawText(f, hintShown, hintX, titleRow.y() + 2, FAINT, false);
        }
    }

    void render(DrawContext c, int mouseX, int mouseY, @Nullable UUID selected, boolean hoverEnabled,
                ReplayHover hover, ReplayTooltips tooltips) {
        ReplayPaint.well(c, viewport);
        if (viewport.height() <= 2 * INSET) {
            return;
        }
        Card hovered = hoverEnabled ? card(mouseX, mouseY) : null;
        c.enableScissor(viewport.x(), viewport.y() + 1, viewport.right(), viewport.bottom() - 1);
        int top = scroll.offset();
        int bottom = top + viewport.height();
        for (Card card : cards) {
            if (card.y + card.height <= top || card.y >= bottom) {
                continue;
            }
            boolean isSelected = card.player.uuid().equals(selected);
            drawCard(c, card, viewport.y() - top + card.y, card == hovered, isSelected, selected != null && !isSelected);
        }
        ReplayPaint.scrollFades(c, new ReplayRect(viewport.x() + 1, viewport.y() + 1,
                viewport.width() - ReplayPaint.SCROLLBAR_GUTTER - 2, viewport.height() - 2), scroll, WELL_RGB);
        c.disableScissor();
        if (scroll.scrollable()) {
            boolean hot = scroll.dragging() || scrollbarHit().contains(mouseX, mouseY);
            int track = track();
            ReplayPaint.scrollbar(c, viewport.right() - 4, trackTop(), track, scroll.thumbTop(trackTop(), track),
                    scroll.thumbHeight(track), hot);
        }
        if (hovered != null) {
            int cardTop = viewport.y() - top + hovered.y;
            ReplaySnapshot.Badge badge = hovered.badgeAt(mouseX - hovered.x, mouseY - cardTop);
            hover.lines(badge != null ? tooltips.badge(badge) : tooltips.player(hovered.player));
        }
    }

    private void drawCard(DrawContext c, Card card, int y, boolean hovered, boolean selected, boolean dimmed) {
        int x = card.x;
        int w = card.width;
        int h = card.height;
        if (selected) {
            ReplayPaint.roundedFill(c, x, y, w, h, SELECT);
            ReplayPaint.roundedOutline(c, x, y, w, h, BRASS);
            c.fill(x + 1, y + 2, x + 3, y + h - 2, COIN);
        } else {
            ReplayPaint.roundedFill(c, x, y, w, h, CARD);
            if (hovered) {
                ReplayPaint.roundedFill(c, x, y, w, h, HOVER);
                ReplayPaint.roundedOutline(c, x, y, w, h, HOVER_EDGE);
            }
        }

        // Head with a dark frame; dead and departed players are dimmed. 带深色边框的头像；死亡与离开的玩家变暗。
        c.fill(x + PAD - 1, y + PAD - 1, x + PAD + HEAD + 1, y + PAD + HEAD + 1, EDGE);
        PlayerSkinDrawer.draw(c, skin(card.player.uuid()), x + PAD, y + PAD, HEAD);
        if (card.player.presence() == ReplaySnapshot.Presence.DEAD) {
            c.fill(x + PAD, y + PAD, x + PAD + HEAD, y + PAD + HEAD, 0x8C1A0606);
        } else if (card.player.presence() == ReplaySnapshot.Presence.LEFT) {
            c.fill(x + PAD, y + PAD, x + PAD + HEAD, y + PAD + HEAD, 0xA0101010);
        }

        c.drawText(f, card.name, x + TEXT_X, y + NAME_Y, card.nameColor, true);
        drawStatus(c, card, x + w - PAD, y);

        int chipY = y + ROLE_Y;
        for (Segment segment : card.roles) {
            int sx = x + segment.x;
            if (segment.label == null) {
                c.drawText(f, segment.glyph, sx, chipY + 1, FAINT, false);
            } else {
                ReplayPaint.roundedFill(c, sx, chipY, segment.width, CHIP_H, withAlpha(segment.color, 0x38));
                c.drawText(f, segment.label, sx + 2, chipY + 1, readable(segment.color), false);
            }
        }

        for (int i = 0; i < card.badgeShown; i++) {
            ReplaySnapshot.Badge badge = card.player.badges().get(i);
            int bx = x + PAD + card.badgeX[i];
            int by = y + BADGE_Y + card.badgeRow[i] * BADGE_ROW;
            ReplayPaint.roundedFill(c, bx, by, card.badgeW[i], BADGE_H, withAlpha(badge.color(), 0x40));
            ReplayPaint.roundedOutline(c, bx, by, card.badgeW[i], BADGE_H, withAlpha(badge.color(), 0x8C));
            c.drawText(f, card.badgeLabel[i], bx + 3, by + 1, readable(badge.color()), false);
        }
        if (card.overflowLabel != null) {
            int bx = x + PAD + card.overflowX;
            int by = y + BADGE_Y + card.overflowRow * BADGE_ROW;
            ReplayPaint.roundedFill(c, bx, by, card.overflowW, BADGE_H, 0x30FFFFFF);
            c.drawText(f, card.overflowLabel, bx + 3, by + 1, MUTED, false);
        }

        if (dimmed) {
            ReplayPaint.roundedFill(c, x, y, w, h, DIM);
        }
    }

    private void drawStatus(DrawContext c, Card card, int right, int y) {
        int x = right - card.statusWidth;
        switch (card.player.presence()) {
            case ALIVE -> ReplayPaint.check(c, x, y + NAME_Y + 1, ALIVE);
            case DEAD -> {
                ReplayPaint.skull(c, x, y + NAME_Y, DEATH);
                if (card.deathTime != null) {
                    c.drawText(f, card.deathTime, x + ReplayPaint.ICON + 2, y + NAME_Y, DEAD_TEXT, false);
                }
            }
            case LEFT -> ReplayPaint.exit(c, x, y + NAME_Y, LEFT);
        }
        if (card.player.result() == ReplaySnapshot.Result.WIN) {
            ReplayPaint.star(c, x - 3 - ReplayPaint.ICON, y + NAME_Y, WIN);
        }
    }

    /** Final role colour; the codec guarantees a role, but a locally built snapshot might not. 最终身份颜色。 */
    static int finalColor(ReplaySnapshot.Player player) {
        return player.roles().isEmpty() ? 0xFFFFFF : player.finalRole().color();
    }

    private static SkinTextures skin(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
        PlayerListEntry entry = handler == null ? null : handler.getPlayerListEntry(uuid);
        return entry != null ? entry.getSkinTextures() : DefaultSkinHelper.getSkinTextures(uuid);
    }

    // ================================================================ card model / 卡片模型

    /** Role chip (label != null) or a gray arrow / ellipsis glyph. 身份小标签（label 非空）或灰色箭头 / 省略号。 */
    private record Segment(int x, int width, @Nullable OrderedText label, int color, String glyph) {
    }

    private final class Card {
        final ReplaySnapshot.Player player;
        final int x;
        final int y;
        final int width;
        int height;
        final String name;
        final int nameColor;
        final @Nullable String deathTime;
        final int statusWidth;
        final List<Segment> roles;
        final int[] badgeX;
        final int[] badgeRow;
        final int[] badgeW;
        final OrderedText[] badgeLabel;
        final int badgeShown;
        final @Nullable String overflowLabel;
        final int overflowX;
        final int overflowRow;
        final int overflowW;

        /**
         * {@code compact} drops the badge rows for the short stacked strip; badges stay in the tooltip.
         * {@code compact} 用于较矮的叠放条带，不显示词条行；词条仍在悬停提示中。
         */
        Card(ReplaySnapshot.Player player, int x, int y, int width, boolean compact) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.width = width;
            this.nameColor = readable(finalColor(player));
            this.deathTime = player.presence() == ReplaySnapshot.Presence.DEAD && player.deathSecond() >= 0
                    ? ReplayTimeFormat.clock(player.deathSecond()) : null;
            int status = ReplayPaint.ICON + (deathTime != null ? 2 + f.getWidth(deathTime) : 0);
            this.statusWidth = status;
            int marks = status + (player.result() == ReplaySnapshot.Result.WIN ? 3 + ReplayPaint.ICON : 0);
            this.name = ReplayPaint.ellipsize(f, player.name(), Math.max(0, width - PAD - marks - 4 - TEXT_X));
            this.roles = roleSegments(player.roles(), Math.max(0, width - PAD - TEXT_X));

            List<ReplaySnapshot.Badge> badges = compact ? List.of() : player.badges();
            int room = Math.max(1, width - 2 * PAD);
            int[] widths = new int[badges.size()];
            badgeLabel = new OrderedText[badges.size()];
            for (int i = 0; i < badges.size(); i++) {
                badgeLabel[i] = ReplayPaint.ellipsize(f, badges.get(i).label(), room - 6);
                widths[i] = f.getWidth(badgeLabel[i]) + 6;
            }
            String overflowProbe = "+" + badges.size();
            ReplayFlow.Result flow = ReplayFlow.layout(widths, room, 2, MAX_BADGE_ROWS, f.getWidth(overflowProbe) + 6);
            badgeX = flow.x();
            badgeRow = flow.row();
            badgeW = widths;
            badgeShown = flow.shown();
            if (flow.overflowed()) {
                overflowLabel = "+" + (badges.size() - flow.shown());
                overflowX = flow.overflowX();
                overflowRow = flow.overflowRow();
                overflowW = f.getWidth(overflowLabel) + 6;
            } else {
                overflowLabel = null;
                overflowX = 0;
                overflowRow = 0;
                overflowW = 0;
            }
            this.height = flow.rows() == 0 ? BASE_HEIGHT : BADGE_Y + flow.rows() * BADGE_ROW - 1 + PAD - 1;
        }

        @Nullable ReplaySnapshot.Badge badgeAt(double localX, double localY) {
            for (int i = 0; i < badgeShown; i++) {
                int bx = PAD + badgeX[i];
                int by = BADGE_Y + badgeRow[i] * BADGE_ROW;
                if (localX >= bx && localX < bx + badgeW[i] && localY >= by && localY < by + BADGE_H) {
                    return player.badges().get(i);
                }
            }
            return null;
        }

        /**
         * Full chain when it fits, else "first → … → last", else "… → last", else the last role alone (ellipsized).
         * 放得下时显示完整身份链，否则依次退化为“首 → … → 末”“… → 末”，最后只显示末身份（必要时省略）。
         */
        private List<Segment> roleSegments(List<ReplaySnapshot.RoleStep> steps, int room) {
            int n = steps.size();
            if (n == 0) {
                return List.of();
            }
            int arrow = f.getWidth(ARROW) + 4;
            int ellipsis = f.getWidth(ReplayPaint.ELLIPSIS) + 4;
            int[] chip = new int[n];
            int full = 0;
            for (int i = 0; i < n; i++) {
                chip[i] = f.getWidth(steps.get(i).label()) + 4;
                full += chip[i] + (i > 0 ? arrow : 0);
            }
            if (full <= room) {
                Segment[] out = new Segment[Math.max(0, 2 * n - 1)];
                int cx = TEXT_X;
                for (int i = 0; i < n; i++) {
                    if (i > 0) {
                        out[2 * i - 1] = glyph(cx, ARROW);
                        cx += arrow;
                    }
                    out[2 * i] = chip(cx, chip[i], steps.get(i));
                    cx += chip[i];
                }
                return List.of(out);
            }
            ReplaySnapshot.RoleStep last = steps.get(n - 1);
            if (n >= 3 && chip[0] + arrow + ellipsis + arrow + chip[n - 1] <= room) {
                int cx = TEXT_X;
                Segment first = chip(cx, chip[0], steps.get(0));
                cx += chip[0];
                Segment a1 = glyph(cx, ARROW);
                cx += arrow;
                Segment dots = glyph(cx, ReplayPaint.ELLIPSIS);
                cx += ellipsis;
                Segment a2 = glyph(cx, ARROW);
                cx += arrow;
                return List.of(first, a1, dots, a2, chip(cx, chip[n - 1], last));
            }
            if (n >= 2 && ellipsis + arrow + chip[n - 1] <= room) {
                int cx = TEXT_X;
                Segment dots = glyph(cx, ReplayPaint.ELLIPSIS);
                cx += ellipsis;
                Segment a = glyph(cx, ARROW);
                cx += arrow;
                return List.of(dots, a, chip(cx, chip[n - 1], last));
            }
            int width = Math.min(chip[n - 1], room);
            return width < 8 ? List.of() : List.of(chip(TEXT_X, width, last));
        }

        private Segment chip(int cx, int width, ReplaySnapshot.RoleStep step) {
            return new Segment(cx, width, ReplayPaint.ellipsize(f, step.label(), width - 4), step.color(), "");
        }

        private Segment glyph(int cx, String glyph) {
            return new Segment(cx + 2, f.getWidth(glyph), null, 0, glyph);
        }
    }
}
