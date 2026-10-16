package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.GlowInkSacItem;
import net.minecraft.item.InkSacItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Map-editing aid: vanilla refuses dye, glow ink and ink sacs on a waxed sign, which can never be unwaxed. A creative
 * player now uses them as on an unwaxed sign (click commands run once, as vanilla); the sign stays waxed and its text
 * locked. Adventure players in a round are unaffected. Hanging signs reach this method through {@code super}.
 * 修图辅助：原版不允许对已涂蜡的告示牌使用染料、荧光墨囊和墨囊，且告示牌无法脱蜡。现在创造模式玩家可像对未涂蜡告示牌一样
 * 使用它们（点击命令照原版执行一次）；告示牌仍保持涂蜡、文字仍锁定。对局中的冒险模式玩家不受影响。悬挂告示牌经 {@code super} 进入。
 */
@Mixin(AbstractSignBlock.class)
public abstract class WaxedSignRecolorMixin {
    @WrapOperation(
            method = "onUseWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;isWaxed()Z")
    )
    private boolean sparkfactionapi$letCreativeRecolorWaxedSign(
            SignBlockEntity sign,
            Operation<Boolean> original,
            @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true) PlayerEntity player
    ) {
        if (player.isCreative() && sparkfactionapi$isRecolorItem(stack.getItem())) {
            return false;
        }
        return original.call(sign);
    }

    // Honeycomb is left out on purpose: waxing stays vanilla. / 刻意不含蜜脾：涂蜡行为保持原版。
    private static boolean sparkfactionapi$isRecolorItem(Item item) {
        return item instanceof DyeItem || item instanceof GlowInkSacItem || item instanceof InkSacItem;
    }
}
