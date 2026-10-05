package dev.caecorthus.sparkfactionapi.client.replay;

/**
 * Integer screen rectangle in scaled GUI pixels; {@code right()} and {@code bottom()} are exclusive, like
 * {@code DrawContext#fill}. Negative sizes are clamped to zero.
 * 缩放后 GUI 像素中的整数矩形；{@code right()} 与 {@code bottom()} 为开区间，与 {@code DrawContext#fill} 一致。负尺寸按 0 处理。
 */
record ReplayRect(int x, int y, int width, int height) {
    static final ReplayRect EMPTY = new ReplayRect(0, 0, 0, 0);

    ReplayRect {
        width = Math.max(0, width);
        height = Math.max(0, height);
    }

    int right() {
        return x + width;
    }

    int bottom() {
        return y + height;
    }

    boolean isEmpty() {
        return width == 0 || height == 0;
    }

    boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    boolean intersects(ReplayRect other) {
        return !isEmpty() && !other.isEmpty()
                && x < other.right() && other.x < right()
                && y < other.bottom() && other.y < bottom();
    }

    boolean containsRect(ReplayRect other) {
        return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
    }

    ReplayRect inset(int amount) {
        return new ReplayRect(x + amount, y + amount, width - 2 * amount, height - 2 * amount);
    }
}
