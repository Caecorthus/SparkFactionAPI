package dev.caecorthus.sparkfactionapi.client.replay;

/**
 * Vertical scroll state of one viewport plus its slim scrollbar geometry and thumb dragging.
 * 单个视口的纵向滚动状态，以及细滚动条的几何与拖动。
 */
final class ReplayScroll {
    static final int MIN_THUMB = 12;

    private int offset;
    private int content;
    private int viewport;
    private boolean dragging;
    private double grab;

    int offset() {
        return offset;
    }

    int max() {
        return Math.max(0, content - viewport);
    }

    boolean scrollable() {
        return content > viewport && viewport > 0;
    }

    void setBounds(int contentHeight, int viewportHeight) {
        content = Math.max(0, contentHeight);
        viewport = Math.max(0, viewportHeight);
        offset = clamp(offset);
    }

    void scrollTo(int value) {
        offset = clamp(value);
    }

    void scrollBy(double amount) {
        scrollTo(offset + (int) Math.round(amount));
    }

    // ---- scrollbar / 滚动条

    int thumbHeight(int track) {
        if (!scrollable()) {
            return track;
        }
        return Math.min(track, Math.max(MIN_THUMB, (int) ((long) track * viewport / Math.max(1, content))));
    }

    int thumbTop(int trackTop, int track) {
        int thumb = thumbHeight(track);
        int range = max();
        return range == 0 ? trackTop : trackTop + (int) ((long) offset * (track - thumb) / range);
    }

    boolean dragging() {
        return dragging;
    }

    /**
     * Starts a drag at {@code mouseY}; clicking the track outside the thumb first centres the thumb there.
     * 在 {@code mouseY} 处开始拖动；点击滑块之外的轨道时先把滑块中心移到该处。
     */
    void beginDrag(double mouseY, int trackTop, int track) {
        int thumb = thumbHeight(track);
        int top = thumbTop(trackTop, track);
        if (mouseY < top || mouseY >= top + thumb) {
            dragTo(mouseY - thumb / 2.0, trackTop, track);
            top = thumbTop(trackTop, track);
        }
        grab = mouseY - top;
        dragging = true;
    }

    void drag(double mouseY, int trackTop, int track) {
        if (dragging) {
            dragTo(mouseY - grab, trackTop, track);
        }
    }

    void endDrag() {
        dragging = false;
    }

    private void dragTo(double thumbTop, int trackTop, int track) {
        int free = track - thumbHeight(track);
        if (free <= 0) {
            return;
        }
        double fraction = (thumbTop - trackTop) / free;
        scrollTo((int) Math.round(Math.max(0, Math.min(1, fraction)) * max()));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(max(), value));
    }
}
