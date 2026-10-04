package dev.caecorthus.sparkfactionapi.client.inventory;

/**
 * Gradient map that turns the gold-and-brown Wathe strip into the blue second row: each opaque pixel keeps its alpha
 * and its relative brightness, so the darkest slot fill becomes deep blue and the brightest frame lines light blue.
 * Pixels are 1.21.1 {@code NativeImage} ABGR ints.
 * 把金棕色的 Wathe 背包条映射为蓝色第二行的渐变映射：每个不透明像素保留透明度与相对亮度，最暗的槽位底色变为深蓝，
 * 最亮的边框线条变为淡蓝。像素为 1.21.1 {@code NativeImage} 的 ABGR 整数。
 */
public final class SecondRowPalette {
    private static final float[] STOP_POSITIONS = {0.0F, 0.35F, 1.0F};
    private static final int[] STOP_COLORS_RGB = {0x071028, 0x265496, 0xB2E0FF};

    private SecondRowPalette() {
    }

    public static int alpha(int abgr) {
        return abgr >>> 24;
    }

    public static float luminance(int abgr) {
        int red = abgr & 0xFF;
        int green = (abgr >>> 8) & 0xFF;
        int blue = (abgr >>> 16) & 0xFF;
        return 0.2126F * red + 0.7152F * green + 0.0722F * blue;
    }

    /**
     * Maps one pixel given the darkest and brightest opaque luminance of the source strip; transparent pixels pass through.
     * 依据源背包条中不透明像素的最暗与最亮亮度映射单个像素；透明像素原样返回。
     */
    public static int recolor(int abgr, float darkest, float brightest) {
        if (alpha(abgr) == 0) {
            return abgr;
        }
        float range = brightest - darkest;
        float position = range > 0.0F ? (luminance(abgr) - darkest) / range : 0.5F;
        int rgb = gradient(Math.max(0.0F, Math.min(1.0F, position)));
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return (abgr & 0xFF000000) | (blue << 16) | (green << 8) | red;
    }

    private static int gradient(float position) {
        for (int stop = 1; stop < STOP_POSITIONS.length; stop++) {
            if (position <= STOP_POSITIONS[stop]) {
                float start = STOP_POSITIONS[stop - 1];
                float blend = (position - start) / (STOP_POSITIONS[stop] - start);
                return lerp(STOP_COLORS_RGB[stop - 1], STOP_COLORS_RGB[stop], blend);
            }
        }
        return STOP_COLORS_RGB[STOP_COLORS_RGB.length - 1];
    }

    private static int lerp(int fromRgb, int toRgb, float blend) {
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int from = (fromRgb >>> shift) & 0xFF;
            int to = (toRgb >>> shift) & 0xFF;
            result |= Math.round(from + (to - from) * blend) << shift;
        }
        return result;
    }
}
