package dev.caecorthus.sparkfactionapi.mixin.inventory;

import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryRows;
import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryScope;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While Wathe's limited inventory is in use (alive, survival, train HUD on, which includes the lobby), vanilla insertion
 * (pickups, giveItemStack, insertStack, offerOrDrop) fills the visible second row before the hidden main slots. Only the
 * order changes; capacity and merging into existing stacks stay vanilla.
 * 使用 Wathe 受限物品栏时（存活、生存模式、列车 HUD 开启，大厅也包括在内），原版插入（拾取、giveItemStack、
 * insertStack、offerOrDrop）先填可见的第二行，再填隐藏主背包槽位。只改变顺序；容量与合并已有堆叠的行为保持原版。
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryRowOrderMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow
    @Final
    public DefaultedList<ItemStack> main;

    @Inject(method = "getEmptySlot", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$preferVisibleSecondRow(CallbackInfoReturnable<Integer> cir) {
        if (LimitedInventoryScope.appliesTo(this.player)) {
            cir.setReturnValue(LimitedInventoryRows.firstEmptySlot(this.main.size(), index -> this.main.get(index).isEmpty()));
        }
    }
}
