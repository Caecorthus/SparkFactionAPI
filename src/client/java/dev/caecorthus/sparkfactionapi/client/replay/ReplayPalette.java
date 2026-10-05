package dev.caecorthus.sparkfactionapi.client.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;

/**
 * Replay screen colours (ARGB). Frame, brass and text values are copied from the Spark "Harpy Express" tokens
 * (SparkAssist {@code ExpressPalette}) so the screen matches the guidebook and inventory cards; this mod does not
 * depend on SparkAssist.
 * 回放界面配色（ARGB）。边框、黄铜与文字颜色复制自 Spark“哈比特快”设计令牌（SparkAssist {@code ExpressPalette}），
 * 与图鉴、物品信息卡保持一致；本模组不依赖 SparkAssist。
 */
final class ReplayPalette {
    // ---- mahogany frame / 桃花心木边框
    static final int SHADOW = 0x66000000;
    static final int EDGE = 0xFF0B0402;
    static final int RIM_HI = 0xFF5A2D19;
    static final int RIM_LO = 0xFF2C1204;
    static final int RIM_MITER = 0xFF43200F;
    static final int BRASS_LO = 0xFF815A15;
    static final int BRASS = 0xFFA58224;
    static final int BRASS_HI = 0xFFC5A244;
    static final int POLISHED = 0xFFD4AF37;
    static final int COIN = 0xFFFFBF49;
    /** Panel body; slightly translucent so the blurred world still shows. 面板底色，略透明以透出模糊的世界。 */
    static final int PANEL = 0xF01C0C05;
    static final int BAND = 0xFF381406;
    static final int WELL = 0xC80C0502;
    /** Approximate colour of a well over the panel, for fades. 凹槽叠在面板上的近似颜色，用于渐隐。 */
    static final int WELL_RGB = 0x0F0704;
    static final int WELL_LIP = 0xFF3D1E0E;
    static final int SELECT = 0xFF4E2614;
    static final int HOVER = 0x16FFBF49;
    static final int HOVER_EDGE = 0x50FFBF49;
    static final int FOCUS = 0xC0C5A244;
    static final int TIP_BG = 0xF5160902;
    static final int ETCH_LIGHT = 0x80542818;
    static final int THUMB_SHADE = 0xFF5E3F0C;
    static final int BUTTON_HOVER = 0xFF3A1A0B;
    static final int CARD = 0x900C0502;
    static final int DIM = 0x70100804;
    static final int ZEBRA = 0x0CFFE6C8;

    // ---- text on dark / 暗底文字
    static final int TEXT = 0xFFEFE2C8;
    static final int TEXT_HI = 0xFFFFF7E6;
    static final int MUTED = 0xFFB4A080;
    static final int FAINT = 0xFF9A8565;
    static final int TIP_DESC = 0xFFCDBB9C;
    static final int HEADING = BRASS_HI;
    static final int TITLE = POLISHED;

    // ---- status / 状态
    static final int ALIVE = 0xFFA9E98C;
    static final int DEAD_TEXT = 0xFFF2838B;
    static final int LEFT = 0xFF8F8478;
    static final int WIN = COIN;

    // ---- categories / 分类
    static final int DEATH = 0xFFE0464E;
    static final int DEATH_ROW = 0x1CE0464E;
    static final int CONVERSION = 0xFFB98CFF;
    static final int CONVERSION_ROW = 0x2E8F5CE6;
    static final int SKILL = 0xFF7CC8E8;
    static final int SHOP = COIN;
    static final int ITEM = 0xFF9AD08A;
    static final int OTHER = FAINT;

    private ReplayPalette() {
    }

    static int category(ReplaySnapshot.Category category) {
        return switch (category) {
            case DEATH -> DEATH;
            case CONVERSION -> CONVERSION;
            case SKILL -> SKILL;
            case SHOP -> SHOP;
            case ITEM -> ITEM;
            case OTHER -> OTHER;
        };
    }

    /**
     * Glyph colour in the timeline: deaths and role changes stay vivid, the rest are muted toward {@link #FAINT}.
     * 时间线图标颜色：死亡与身份转化保持醒目，其余分类向 FAINT 收敛。
     */
    static int glyph(ReplaySnapshot.Category category) {
        int color = category(category);
        return category == ReplaySnapshot.Category.DEATH || category == ReplaySnapshot.Category.CONVERSION
                ? color : mix(color, FAINT, 0.4);
    }

    /** Opaque ARGB from a server RGB (alpha bits ignored). 由服务端 RGB 得到不透明 ARGB（忽略透明位）。 */
    static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Per-channel ARGB lerp, rounded. 按通道线性插值并四舍五入。 */
    static int mix(int a, int b, double t) {
        int alpha = (int) Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        int red = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int green = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int blue = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Lifts very dark colours toward the text colour so role and badge labels stay readable on the dark panel.
     * 把过暗的颜色向文字色提亮，保证身份与词条标签在暗色面板上可读。
     */
    static int readable(int rgb) {
        int argb = opaque(rgb);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        double luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luma >= 90 ? argb : mix(argb, TEXT_HI, Math.min(0.75, (90 - luma) / 120.0 + 0.25));
    }
}
