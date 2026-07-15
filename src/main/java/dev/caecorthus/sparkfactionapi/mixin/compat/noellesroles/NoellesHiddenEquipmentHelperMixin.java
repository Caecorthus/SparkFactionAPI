package dev.caecorthus.sparkfactionapi.mixin.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesHiddenEquipmentRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends NoellesRoles' hidden-equipment decision without making it a hard dependency.
 * 在不硬依赖 NoellesRoles 的前提下，扩展其隐藏装备判定。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.util.HiddenEquipmentHelper", remap = false)
public abstract class NoellesHiddenEquipmentHelperMixin {
    @Inject(method = "shouldHideItem", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void sparkfactionapi$includeRegisteredItems(
            ItemStack stack,
            PlayerEntity holder,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && NoellesHiddenEquipmentRegistry.shouldHide(stack)) {
            cir.setReturnValue(true);
        }
    }
}
