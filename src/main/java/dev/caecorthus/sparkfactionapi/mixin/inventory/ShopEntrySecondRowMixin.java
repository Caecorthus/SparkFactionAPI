package dev.caecorthus.sparkfactionapi.mixin.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryRows;
import dev.caecorthus.sparkfactionapi.impl.inventory.LimitedInventoryScope;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A default shop purchase whose hotbar is full lands in the visible second row. Only the default {@code onBuy} path is
 * widened: the static {@code insertStackInFreeSlot} stays hotbar-only because the psycho bat and other callers need the
 * hotbar. Uses {@code setStack} like Wathe, so insertStack guards (e.g. SparkTraits Impostor) keep their scope.
 * 快捷栏已满时，默认商店购买放进可见的第二行。只放宽默认 {@code onBuy} 路径：静态 {@code insertStackInFreeSlot}
 * 仍只用快捷栏，因为狂暴球棒等调用方必须进快捷栏。与 Wathe 一样使用 {@code setStack}，不改变 insertStack 拦截的范围。
 */
@Mixin(ShopEntry.class)
public abstract class ShopEntrySecondRowMixin {
    @WrapOperation(
            method = "onBuy",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/util/ShopEntry;insertStackInFreeSlot(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Z"
            )
    )
    private boolean sparkfactionapi$fallBackToSecondRow(
            PlayerEntity player,
            ItemStack stack,
            Operation<Boolean> original
    ) {
        if (original.call(player, stack)) {
            return true;
        }
        if (!LimitedInventoryScope.appliesTo(player)) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        int slot = LimitedInventoryRows.firstEmptySecondRowSlot(index -> inventory.getStack(index).isEmpty());
        if (slot < 0) {
            return false;
        }
        inventory.setStack(slot, stack);
        return true;
    }
}
