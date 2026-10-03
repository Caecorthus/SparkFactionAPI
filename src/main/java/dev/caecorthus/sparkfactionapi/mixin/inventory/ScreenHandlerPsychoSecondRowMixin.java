package dev.caecorthus.sparkfactionapi.mixin.inventory;

import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryRows;
import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryScope;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While psycho mode runs, Wathe only lets the player hold a hotbar slot with the bat; a bat moved into the second row
 * could not be selected again. Second-row clicks are ignored for that time, on both sides so the client predicts it.
 * 狂暴模式期间，Wathe 只允许玩家选中放着球棒的快捷栏格；球棒若被移入第二行就再也选不中。因此这段时间忽略对第二行的
 * 点击，两端同时生效以便客户端预测一致。
 */
@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerPsychoSecondRowMixin {
    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$freezeSecondRowDuringPsycho(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return;
        }
        Slot slot = this.slots.get(slotIndex);
        if (!(slot.inventory instanceof PlayerInventory) || !LimitedInventoryRows.isSecondRowIndex(slot.getIndex())) {
            return;
        }
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.getNullable(player);
        if (psycho != null && psycho.getPsychoTicks() > 0 && LimitedInventoryScope.appliesTo(player)) {
            ci.cancel();
        }
    }
}
