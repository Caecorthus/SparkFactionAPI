package dev.caecorthus.sparkfactionapi.client.mixin.inventory;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.client.inventory.LimitedInventorySecondRow;
import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryRows;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Adds player-inventory row 27-35 to Wathe's limited inventory. Wathe draws, hit-tests and clicks only slots that
 * {@code isHotbarSlot} accepts, and their vanilla slot y (120) already sits one row above the hotbar (142).
 * 为 Wathe 受限物品栏加入玩家背包 27-35 行。Wathe 只绘制、命中检测并点击 {@code isHotbarSlot} 接受的槽位，
 * 而这些槽位的原版 y（120）本就位于快捷栏（142）上方一行。
 */
@Mixin(LimitedHandledScreen.class)
public abstract class LimitedHandledScreenSecondRowMixin extends Screen {
    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    protected LimitedHandledScreenSecondRowMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract List<Text> getTooltipFromItem(ItemStack stack);

    @Inject(method = "isHotbarSlot", at = @At("HEAD"), cancellable = true)
    private static void sparkfactionapi$acceptSecondRow(Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot.inventory instanceof PlayerInventory && LimitedInventoryRows.isSecondRowIndex(slot.getIndex())) {
            cir.setReturnValue(true);
        }
    }

    // Keep Yarn: init overrides Screen.init; remapJar rewrites the selector to its intermediary name. init re-runs after
    // clearChildren() on resize, so the bounds widget is rebuilt with the strip.
    // 保持 Yarn：init 覆写 Screen.init，remapJar 会把选择器改写为 intermediary 名称。缩放时会在 clearChildren() 后重新
    // 执行 init，占位控件随条一起重建。
    @Inject(method = "init()V", at = @At("TAIL"))
    private void sparkfactionapi$reserveSecondRow(CallbackInfo ci) {
        this.addDrawableChild(new LimitedInventorySecondRow.Bounds(this.x, this.y, this.backgroundWidth));
    }

    /**
     * With the Touchscreen option, Wathe closes the screen on a tap outside its 176x32 strip; the second row is inside.
     * 开启触屏选项时，Wathe 会在点到 176x32 条外时关闭界面；第二行属于界面内部。
     */
    @ModifyReturnValue(method = "isClickOutsideBounds", at = @At("RETURN"))
    private boolean sparkfactionapi$secondRowIsInside(boolean outside, double mouseX, double mouseY, int left, int top,
                                                     int button) {
        return outside && !LimitedInventorySecondRow.contains(mouseX, mouseY, left, top, this.backgroundWidth);
    }

    /**
     * Wathe draws the item name above the strip, where the second row now sits, and the description below it. Draw
     * name and description as one tooltip at Wathe's description anchor instead, so the second row stays visible.
     * Wathe 把物品名画在条上方（现在是第二行的位置），描述画在下方。改为在 Wathe 描述锚点处把名称与描述画成一个提示框，
     * 以免遮住第二行。
     */
    @Inject(method = "renderLimitedInventoryTooltip", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$drawTooltipBelowStrip(DrawContext context, ItemStack stack, CallbackInfo ci) {
        ci.cancel();
        List<Text> lines = this.getTooltipFromItem(stack);
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, this.textRenderer.getWidth(line));
        }
        context.drawTooltip(this.textRenderer, lines, stack.getTooltipData(),
                this.x + LimitedInventorySecondRow.TOOLTIP_CENTER_X_OFFSET - width / 2,
                this.y + LimitedInventorySecondRow.TOOLTIP_Y_OFFSET);
    }
}
