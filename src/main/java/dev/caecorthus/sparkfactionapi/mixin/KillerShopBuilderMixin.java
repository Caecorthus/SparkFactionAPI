package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.shop.FactionShopAccessRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.KillerShopBuilder;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Routes killer shop access through the explicit faction capability.
 * 将杀手商店访问改为显式阵营能力判断。
 *
 * <p>A {@code @WrapOperation} that never calls the original: the answer is the same as the former {@code @Redirect},
 * but the call site stays chainable for other mods.
 * 使用从不调用原方法的 {@code @WrapOperation}：结果与原先的 {@code @Redirect} 相同，但调用点对其他模组仍可链式叠加。
 */
@Mixin(KillerShopBuilder.class)
public abstract class KillerShopBuilderMixin {
    @WrapOperation(
            method = "buildShop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private static boolean sparkfactionapi$useKillerFeatureAccessCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player,
            Operation<Boolean> original
    ) {
        return FactionShopAccessRules.canUseKillerShop(player, gameComponent);
    }
}
