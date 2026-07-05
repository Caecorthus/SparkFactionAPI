package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.shop.FactionShopAccessRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.KillerShopBuilder;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes killer shop access through the explicit faction capability.
 * 将杀手商店访问改为显式阵营能力判断。
 */
@Mixin(KillerShopBuilder.class)
public abstract class KillerShopBuilderMixin {
    @Redirect(
            method = "buildShop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private static boolean sparkfactionapi$useKillerFeatureAccessCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player
    ) {
        return FactionShopAccessRules.canUseKillerShop(player, gameComponent);
    }
}
