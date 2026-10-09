package dev.caecorthus.sparkfactionapi.mixin.record;

import dev.caecorthus.sparkfactionapi.impl.record.ConsumeRecorder;
import dev.doctor4t.wathe.util.PoisonUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wathe calls {@code applyFoodPoison(target, stack)} whenever a food or drink is consumed, including by players who
 * are fed without a use animation (Noelle's Waiter, SparkStrength capsules). Observes only; poisoning is unchanged.
 * Wathe 在每次食物或饮品被食用时调用 {@code applyFoodPoison(target, stack)}，包括无使用动画的被喂食玩家（Noelle 服务员、
 * SparkStrength 胶囊）。仅观察，不改变下毒逻辑。
 */
@Mixin(value = PoisonUtils.class, remap = false)
public abstract class PoisonUtilsConsumeMixin {
    @Inject(
            method = "applyFoodPoison(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void sparkfactionapi$recordFedConsumption(PlayerEntity target, ItemStack stack, CallbackInfo ci) {
        ConsumeRecorder.onFoodConsumed(target, stack);
    }
}
