package dev.caecorthus.sparkfactionapi.client.replay;

import net.minecraft.client.gui.DrawContext;

/**
 * Vertical scroll state of one viewport plus its slim scrollbar geometry and thumb dragging. The scrollbar sits in the
 * viewport's right gutter ({@link ReplayPaint#SCROLLBAR_GUTTER}) and its track is inset 2 px top and bottom.
 * 单个视口的纵向滚动状态，以及细滚动条的几何与拖动。滚动条位于视口右侧槽（{@link ReplayPaint#SCROLLBAR_GUTTER}）内，
 * 轨道上下各内缩 2 像素。
 */
final class ReplayScroll {
    static final int MIN_THUMB = 12;

    private int offset;
    private int content;
    private ReplayRect viewport = ReplayRect.EMPTY;
    private boolean dragging;
    private double grab;

    int offset() {
        return offset;
    }

    int max() {
        return Math.max(0, content - viewport.height());
    }

    boolean scrollable() {
        return content > viewport.height() && viewport.height() > 0;
    }

    void setBounds(int contentHeight, ReplayRect viewport) {
        content = Math.max(0, contentHeight);
        this.viewport = viewport;
        offset = clamp(offset);
    }

    void scrollTo(int value) {
        offset = clamp(value);
    }

    void scrollBy(double amount) {
        scrollTo(offset + (int) Math.round(amount));
    }

    // ---- scrollbar / 滚动条

    /** Gutter that starts a thumb drag; empty when nothing scrolls. 可开始拖动滑块的右侧槽；无法滚动时为空。 */
    ReplayRect scrollbarHit() {
        if (!scrollable()) {
            return ReplayRect.EMPTY;
        }
        return new ReplayRect(viewport.right() - ReplayPaint.SCROLLBAR_GUTTER - 1, viewport.y(),
                ReplayPaint.SCROLLBAR_GUTTER + 1, viewport.height());
    }

    /**
     * Draws the scrollbar when scrollable, highlighted while dragged or hovered. Callers draw it after disabling their
     * scissor, inside the same batch as the rest of the view.
     * 可滚动时绘制滚动条，拖动或悬停时高亮。调用方在关闭裁剪后、与视图其余部分同一批次内绘制。
     */
    void render(DrawContext c, int mouseX, int mouseY) {
        if (!scrollable()) {
            return;
        }
        boolean hot = dragging || scrollbarHit().contains(mouseX, mouseY);
        int track = track();
        ReplayPaint.scrollbar(c, viewport.right() - 4, trackTop(), track, thumbTop(track), thumbHeight(track), hot);
    }

    private int trackTop() {
        return viewport.y() + 2;
    }

    private int track() {
        return Math.max(0, viewport.height() - 4);
    }

    private int thumbHeight(int track) {
        if (!scrollable()) {
            return track;
        }
        return Math.min(track, Math.max(MIN_THUMB, (int) ((long) track * viewport.height() / Math.max(1, content))));
    }

    private int thumbTop(int track) {
        int thumb = thumbHeight(track);
        int range = max();
        return range == 0 ? trackTop() : trackTop() + (int) ((long) offset * (track - thumb) / range);
    }

    /**
     * Starts a drag at {@code mouseY}; clicking the track outside the thumb first centres the thumb there.
     * 在 {@code mouseY} 处开始拖动；点击滑块之外的轨道时先把滑块中心移到该处。
     */
    void beginDrag(double mouseY) {
        int track = track();
        int thumb = thumbHeight(track);
        int top = thumbTop(track);
        if (mouseY < top || mouseY >= top + thumb) {
            dragTo(mouseY - thumb / 2.0, track);
            top = thumbTop(track);
        }
        grab = mouseY - top;
        dragging = true;
    }

    void drag(double mouseY) {
        if (dragging) {
            dragTo(mouseY - grab, track());
        }
    }

    void endDrag() {
        dragging = false;
    }

    private void dragTo(double thumbTop, int track) {
        int free = track - thumbHeight(track);
        if (free <= 0) {
            return;
        }
        double fraction = (thumbTop - trackTop()) / free;
        scrollTo((int) Math.round(Math.max(0, Math.min(1, fraction)) * max()));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(max(), value));
    }
}
