package dev.caecorthus.sparkfactionapi.client.mixin.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.client.inventory.LimitedInventorySecondRow;
import dev.caecorthus.sparkfactionapi.client.inventory.SecondRowTexture;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Builds the two-row frame from slices of Wathe's own one-row strip texture. The second row uses a blue recolor of that
 * texture; the divider and the hotbar keep Wathe's gold. Resource packs that retexture the strip retexture both rows.
 * 用 Wathe 自身单行背包材质的切片拼出两行边框。第二行使用该材质的蓝色版本，分隔线与快捷栏保留 Wathe 的金色。
 * 资源包替换该材质时两行都会随之改变。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class LimitedInventoryScreenSecondRowMixin {
    // Strip rows (Wathe texture v): 1-6 top frame, 7-24 slot body, 25-30 bottom frame.
    // 背包条纹理的行（v）：1-6 顶边框，7-24 槽位主体，25-30 底边框。
    @Unique
    private static final int SLOT_BODY_TOP = 7;
    @Unique
    private static final int DIVIDER_TOP = 3;

    @WrapOperation(
            method = "drawBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"
            )
    )
    private void sparkfactionapi$drawTwoRowStrip(
            DrawContext context,
            Identifier texture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            Operation<Void> original
    ) {
        // Second row in blue: top frame plus slot body, one row pitch above the hotbar.
        // 蓝色第二行：顶边框加槽位主体，位于快捷栏上方一个行距。
        int upperHeight = LimitedInventorySecondRow.ROW_PITCH + DIVIDER_TOP;
        original.call(context, SecondRowTexture.id(), x, y - LimitedInventorySecondRow.ROW_PITCH,
                u, v, width, upperHeight);
        // Divider: the inner lines of the top frame, between the two slot bodies.
        // 分隔线：顶边框的内侧线条，位于两行槽位之间。
        int dividerHeight = SLOT_BODY_TOP - DIVIDER_TOP;
        original.call(context, texture, x, y + DIVIDER_TOP, u, v + DIVIDER_TOP, width, dividerHeight);
        // Hotbar: slot body and bottom frame, unchanged.
        // 快捷栏：槽位主体与底边框，保持不变。
        original.call(context, texture, x, y + SLOT_BODY_TOP, u, v + SLOT_BODY_TOP, width, height - SLOT_BODY_TOP);
    }
}
