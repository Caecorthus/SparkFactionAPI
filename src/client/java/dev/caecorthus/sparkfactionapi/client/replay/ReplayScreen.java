package dev.caecorthus.sparkfactionapi.client.replay;

import static dev.caecorthus.sparkfactionapi.client.replay.ReplayPalette.*;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Replay screen for one frozen {@link ReplaySnapshot}: header, player roster, filterable and searchable timeline,
 * and a match bar with death / role-change markers. Everything is drawn procedurally inside one panel; scroll areas
 * are scissored. State lives in {@link ReplayTimelineModel}; the views only cache geometry and wrapped text.
 * 单份冻结 {@link ReplaySnapshot} 的回放界面：标题、玩家名单、可筛选与搜索的时间线，以及带死亡 / 身份转化标记的对局时间轴。
 * 全部内容在一个面板内程序化绘制，滚动区域使用裁剪。状态保存在 {@link ReplayTimelineModel} 中，各视图只缓存几何与换行文本。
 */
public class ReplayScreen extends Screen {
    private static final double ROSTER_WHEEL = 24;
    private static final double TIMELINE_WHEEL = 30;
    private static final int SEARCH_MAX_LENGTH = 64;

    private final ReplaySnapshot snapshot;
    private final @Nullable Screen parent;
    private final ReplayTimelineModel model;
    private final ReplayHover hover = new ReplayHover();
    private @Nullable ReplayTooltips tooltips;
    private @Nullable ReplayHeaderView header;
    private @Nullable ReplayRosterView roster;
    private @Nullable ReplayTimelineView timeline;
    private @Nullable ReplayTimelineBar bar;
    private @Nullable ReplayFilterTabs tabs;
    private @Nullable TextFieldWidget searchField;
    private @Nullable ReplayScreenLayout layout;
    private @Nullable ReplayScroll dragging;
    private boolean draggingRoster;
    private boolean scrubbing;
    private int countReserve;
    private OrderedText chipLine = OrderedText.EMPTY;
    private int chipWidth;
    private String countLine = "";
    private int countVersion = -1;
    private String rosterTitle = "";

    public ReplayScreen(ReplaySnapshot snapshot, @Nullable Screen parent) {
        super(Text.translatable("screen.sparkfactionapi.replay.title"));
        this.snapshot = snapshot;
        this.parent = parent;
        this.model = new ReplayTimelineModel(snapshot);
    }

    public ReplaySnapshot snapshot() {
        return snapshot;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    /** The replay is informational; never pause a singleplayer world for it. 回放只用于查看，单人模式下也不暂停。 */
    @Override
    public boolean shouldPause() {
        return false;
    }

    // ================================================================ init / layout 初始化与布局

    @Override
    protected void init() {
        // Views are created on the first init and only re-laid out on resize, so filters, scroll and wrap caches
        // survive; the search widget is recreated because Screen#clearAndInit drops every child.
        // 视图在首次 init 时创建，缩放时只重新布局，筛选、滚动与换行缓存都会保留；搜索框会重建，因为
        // Screen#clearAndInit 会移除所有子元素。
        if (tooltips == null) {
            tooltips = new ReplayTooltips(textRenderer);
            header = new ReplayHeaderView(textRenderer, snapshot);
            roster = new ReplayRosterView(textRenderer, snapshot.players());
            timeline = new ReplayTimelineView(textRenderer, model);
            bar = new ReplayTimelineBar(textRenderer, model, timeline, snapshot.durationSeconds());
            tabs = new ReplayFilterTabs(textRenderer, model);
            countReserve = textRenderer.getWidth(countText(model.total(), model.total()));
        }
        tooltips.setScreenWidth(width);
        rosterTitle = Text.translatable(ReplayTexts.PLAYERS, snapshot.players().size()).getString();

        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, 10, 10,
                Text.translatable(ReplayTexts.SEARCH));
        field.setDrawsBackground(false);
        field.setMaxLength(SEARCH_MAX_LENGTH);
        field.setEditableColor(TEXT);
        field.setPlaceholder(Text.translatable(ReplayTexts.SEARCH).withColor(FAINT & 0xFFFFFF));
        field.setText(model.query());
        field.setChangedListener(this::onSearchChanged);
        searchField = addSelectableChild(field);
        relayout();
    }

    private void relayout() {
        boolean chip = model.playerFilter() != null;
        ReplayScreenLayout probe = ReplayScreenLayout.compute(width, height, 1, chip, countReserve);
        int rows = tabs.rowsFor(probe.timeline().width());
        layout = rows == 1 ? probe : ReplayScreenLayout.compute(width, height, rows, chip, countReserve);

        header.layout(layout);
        tabs.layout(layout.tabs());
        roster.layoutTitle(layout.rosterTitle(), rosterTitle);
        roster.layout(layout.rosterList(), layout.stacked());
        timeline.layout(layout.list());
        timeline.sync();
        bar.layout(layout.bar());
        bar.sync();

        ReplayRect well = layout.search();
        searchField.setX(well.x() + 16);
        searchField.setY(well.y() + (well.height() - 8) / 2);
        searchField.setWidth(Math.max(10, well.width() - 16 - 12));
        searchField.setHeight(Math.max(8, well.height() - (well.height() - 8) / 2));
        searchField.visible = !well.isEmpty();
        layoutChip();
    }

    private void layoutChip() {
        ReplaySnapshot.Player selected = model.player(model.playerFilter());
        UUID filter = model.playerFilter();
        if (filter == null || layout.chip().isEmpty()) {
            chipLine = OrderedText.EMPTY;
            chipWidth = 0;
            return;
        }
        Text name = selected != null
                ? Text.literal(selected.name()).withColor(readable(ReplayRosterView.finalColor(selected)) & 0xFFFFFF)
                : Text.literal(filter.toString().substring(0, 8));
        Text label = Text.translatable(ReplayTexts.PLAYER_FILTER, name);
        int room = Math.max(0, layout.chip().width() - 18);
        chipLine = ReplayPaint.ellipsize(textRenderer, label, room);
        chipWidth = Math.min(layout.chip().width(), textRenderer.getWidth(chipLine) + 18);
    }

    private static Text countText(int shown, int total) {
        return Text.translatable(ReplayTexts.COUNT, shown, total);
    }

    // ================================================================ filters / 筛选

    private void onSearchChanged(String text) {
        model.setQuery(text);
        filtersChanged(false);
    }

    private void togglePlayer(UUID uuid) {
        model.togglePlayerFilter(uuid);
        if (model.playerFilter() != null) {
            roster.reveal(model.playerFilter());
        }
        filtersChanged(true);
    }

    private void filtersChanged(boolean playerChanged) {
        if (playerChanged) {
            // The chip row appears or disappears, which moves the list. 标签行出现或消失会移动列表。
            relayout();
        } else {
            timeline.sync();
            bar.sync();
        }
    }

    // ================================================================ render / 绘制

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Vanilla blur + dim exactly once; the search field is drawn by hand between our layers, not by super.
        // 原版模糊与变暗只做一次；搜索框在各层之间手动绘制，不经由 super。
        renderBackground(context, mouseX, mouseY, delta);
        if (layout == null) {
            return;
        }
        hover.clear();
        // Cheap version checks; a no-op unless a filter changed since the last rebuild. 版本比对，筛选未变时不做任何事。
        timeline.sync();
        bar.sync();
        boolean hoverEnabled = dragging == null && !scrubbing;
        ReplayRect panel = layout.panel();
        ReplayPaint.panel(context, panel);
        ReplayPaint.band(context, panel, layout.header().bottom() + (layout.compact() ? 0 : 1));
        drawSeparators(context);

        header.render(context, mouseX, mouseY, hoverEnabled, hover, tooltips);

        roster.renderTitle(context);
        roster.render(context, mouseX, mouseY, model.playerFilter(), hoverEnabled, hover, tooltips);

        tabs.render(context, mouseX, mouseY, hoverEnabled);
        drawSearch(context, mouseX, mouseY, hoverEnabled);
        searchField.render(context, mouseX, mouseY, delta);
        drawCount(context);
        drawChip(context, mouseX, mouseY, hoverEnabled);

        timeline.render(context, mouseX, mouseY, hoverEnabled, hover, tooltips);
        bar.render(context, mouseX, mouseY, hoverEnabled, hover);

        hover.render(context, textRenderer, mouseX, mouseY, width, height);
    }

    private void drawSeparators(DrawContext c) {
        ReplayRect roster = layout.roster();
        ReplayRect timeline = layout.timeline();
        if (layout.stacked()) {
            int y = (roster.bottom() + timeline.y()) / 2 - 1;
            ReplayPaint.etched(c, roster.x(), roster.right(), y);
        } else {
            int x = (roster.right() + timeline.x()) / 2 - 1;
            ReplayPaint.etchedVertical(c, x, roster.y(), roster.bottom());
        }
        ReplayRect bar = layout.bar();
        ReplayPaint.etched(c, bar.x(), bar.right(), bar.y() - (layout.compact() ? 3 : 4));
    }

    private void drawSearch(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled) {
        ReplayRect well = layout.search();
        if (well.isEmpty()) {
            return;
        }
        boolean focused = searchField.isFocused();
        ReplayPaint.roundedFill(c, well.x(), well.y(), well.width(), well.height(), EDGE);
        c.fill(well.x() + 1, well.y() + 1, well.right() - 1, well.bottom() - 1, 0xFF0C0502);
        c.fill(well.x() + 1, well.bottom() - 2, well.right() - 1, well.bottom() - 1, WELL_LIP);
        if (focused) {
            ReplayPaint.roundedOutline(c, well.x(), well.y(), well.width(), well.height(), FOCUS);
        } else if (hoverEnabled && well.contains(mouseX, mouseY)) {
            ReplayPaint.roundedOutline(c, well.x(), well.y(), well.width(), well.height(), HOVER_EDGE);
        }
        ReplayPaint.magnifier(c, well.x() + 4, well.y() + (well.height() - 9) / 2, focused ? BRASS_HI : FAINT);
        if (!searchField.getText().isEmpty()) {
            ReplayRect clear = clearBox();
            boolean hot = hoverEnabled && clear.contains(mouseX, mouseY);
            ReplayPaint.clear(c, clear.x() + 2, clear.y() + (clear.height() - 5) / 2, hot ? TEXT_HI : FAINT);
        }
    }

    private ReplayRect clearBox() {
        ReplayRect well = layout.search();
        return new ReplayRect(well.right() - 11, well.y(), 10, well.height());
    }

    private void drawCount(DrawContext c) {
        ReplayRect box = layout.count();
        if (box.isEmpty()) {
            return;
        }
        if (countVersion != model.version()) {
            countLine = countText(model.visibleCount(), model.total()).getString();
            countVersion = model.version();
        }
        String shown = ReplayPaint.ellipsize(textRenderer, countLine, box.width());
        c.drawText(textRenderer, shown, box.right() - textRenderer.getWidth(shown), box.y() + (box.height() - 8) / 2,
                FAINT, false);
    }

    private void drawChip(DrawContext c, int mouseX, int mouseY, boolean hoverEnabled) {
        ReplayRect row = layout.chip();
        if (row.isEmpty() || chipWidth == 0) {
            return;
        }
        boolean hot = hoverEnabled && chipBox().contains(mouseX, mouseY);
        ReplayPaint.roundedFill(c, row.x(), row.y(), chipWidth, row.height(), hot ? BUTTON_HOVER : SELECT);
        ReplayPaint.roundedOutline(c, row.x(), row.y(), chipWidth, row.height(), hot ? COIN : BRASS);
        c.fill(row.x() + 1, row.y() + 2, row.x() + 3, row.bottom() - 2, COIN);
        c.drawText(textRenderer, chipLine, row.x() + 6, row.y() + (row.height() - 8) / 2 + 1, TEXT, false);
        ReplayPaint.clear(c, row.x() + chipWidth - 9, row.y() + (row.height() - 5) / 2, hot ? TEXT_HI : MUTED);
    }

    private ReplayRect chipBox() {
        ReplayRect row = layout.chip();
        return new ReplayRect(row.x(), row.y(), chipWidth, row.height());
    }

    // ================================================================ input / 输入

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (layout == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (header.closeHit(mouseX, mouseY)) {
            playClick();
            close();
            return true;
        }
        ReplayRect well = layout.search();
        if (well.contains(mouseX, mouseY)) {
            if (!searchField.getText().isEmpty() && clearBox().contains(mouseX, mouseY)) {
                searchField.setText("");
            }
            setFocused(searchField);
            searchField.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        setFocused(null);

        if (beginDrag(roster.scroll(), roster.scrollbarHit(), roster.trackTop(), roster.track(), mouseX, mouseY)) {
            draggingRoster = true;
            return true;
        }
        if (beginDrag(timeline.scroll(), timeline.scrollbarHit(), timeline.trackTop(), timeline.track(), mouseX,
                mouseY)) {
            draggingRoster = false;
            return true;
        }
        if (tabs.click(mouseX, mouseY)) {
            playClick();
            filtersChanged(false);
            return true;
        }
        if (chipWidth > 0 && chipBox().contains(mouseX, mouseY)) {
            playClick();
            model.setPlayerFilter(null);
            filtersChanged(true);
            return true;
        }
        UUID card = roster.cardAt(mouseX, mouseY);
        if (card != null) {
            playClick();
            togglePlayer(card);
            return true;
        }
        UUID named = timeline.nameAt(mouseX, mouseY);
        if (named != null && model.player(named) != null) {
            playClick();
            togglePlayer(named);
            return true;
        }
        int marker = bar.markerAt(mouseX, mouseY);
        if (marker >= 0) {
            timeline.reveal(marker);
            return true;
        }
        if (bar.trackHit(mouseX, mouseY)) {
            // Clicking or dragging along the track scrubs the list to that time. 在轨道上点击或拖动可按时间跳转列表。
            scrubbing = true;
            timeline.revealSecond(bar.secondAt(mouseX));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean beginDrag(ReplayScroll scroll, ReplayRect hit, int trackTop, int track, double mouseX,
                              double mouseY) {
        if (!hit.contains(mouseX, mouseY)) {
            return false;
        }
        scroll.beginDrag(mouseY, trackTop, track);
        dragging = scroll;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrubbing) {
            timeline.revealSecond(bar.secondAt(mouseX));
            return true;
        }
        if (dragging != null) {
            if (draggingRoster) {
                dragging.drag(mouseY, roster.trackTop(), roster.track());
            } else {
                dragging.drag(mouseY, timeline.trackTop(), timeline.track());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrubbing) {
            scrubbing = false;
            return true;
        }
        if (dragging != null) {
            dragging.endDrag();
            dragging = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (layout != null) {
            if (layout.rosterList().contains(mouseX, mouseY)) {
                roster.scroll().scrollBy(-verticalAmount * ROSTER_WHEEL);
                return true;
            }
            if (layout.list().contains(mouseX, mouseY) || layout.bar().contains(mouseX, mouseY)) {
                timeline.scroll().scrollBy(-verticalAmount * TIMELINE_WHEEL);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc is handled first by Screen#keyPressed, so a focused search field never swallows it.
        // Esc 由 Screen#keyPressed 最先处理，聚焦的搜索框不会吞掉它。
        if (layout != null && keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            setFocused(searchField);
            return true;
        }
        if (layout != null && getFocused() != searchField) {
            ReplayScroll scroll = timeline.scroll();
            int page = Math.max(10, layout.list().height() - 20);
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> scroll.scrollBy(-ReplayTimelineView.LINE_HEIGHT * 2);
                case GLFW.GLFW_KEY_DOWN -> scroll.scrollBy(ReplayTimelineView.LINE_HEIGHT * 2);
                case GLFW.GLFW_KEY_PAGE_UP -> scroll.scrollBy(-page);
                case GLFW.GLFW_KEY_PAGE_DOWN -> scroll.scrollBy(page);
                case GLFW.GLFW_KEY_HOME -> scroll.scrollTo(0);
                case GLFW.GLFW_KEY_END -> scroll.scrollTo(scroll.max());
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playClick() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
