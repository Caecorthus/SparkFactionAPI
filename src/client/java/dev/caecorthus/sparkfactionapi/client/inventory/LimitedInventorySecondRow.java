package dev.caecorthus.sparkfactionapi.client.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;

/**
 * Screen geometry of the second limited-inventory row, which sits directly above Wathe's 176x32 hotbar strip.
 * 第二行受限物品栏的屏幕几何；它紧贴在 Wathe 176x32 快捷栏条上方。
 */
public final class LimitedInventorySecondRow {
    /** Vanilla y of main row 27-35 (120) is one pitch above the hotbar row (142). / 原版 27-35 行 y（120）比快捷栏（142）高一个行距。 */
    public static final int ROW_PITCH = 22;
    /** The merged item tooltip keeps Wathe's description anchor below the strip. / 合并后的物品提示沿用 Wathe 描述在条下方的锚点。 */
    public static final int TOOLTIP_Y_OFFSET = 50;
    /** Strip centre (88) minus the 12 px that HoveredTooltipPositioner adds, as in Wathe. / 条中心（88）减去定位器右移的 12 像素，与 Wathe 相同。 */
    public static final int TOOLTIP_CENTER_X_OFFSET = 76;

    private LimitedInventorySecondRow() {
    }

    public static boolean contains(double mouseX, double mouseY, int stripX, int stripY, int stripWidth) {
        return mouseX >= stripX && mouseX < stripX + stripWidth
                && mouseY >= stripY - ROW_PITCH && mouseY < stripY;
    }

    /**
     * Inactive, invisible-content widget covering the row. SparkWitch/SparkTraits info cards and the SparkAssist
     * guidebook treat visible widgets as obstacles but hard-code only the old 176x32 strip; this makes them avoid the
     * row without changes on their side. It never takes clicks, focus or narration.
     * 覆盖第二行的非激活、无绘制控件。SparkWitch/SparkTraits 信息卡与 SparkAssist 指南书会避开可见控件，但只写死了
     * 旧的 176x32 条；有了它，这些模组无需改动即可避开第二行。它不接收点击、焦点或旁白。
     */
    public static final class Bounds extends ClickableWidget {
        public Bounds(int stripX, int stripY, int stripWidth) {
            super(stripX, stripY - ROW_PITCH, stripWidth, ROW_PITCH, ScreenTexts.EMPTY);
            this.active = false;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }
}
