package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps shop cooldown sharing tied to killer-feature access.
 * 让商店冷却共享跟随显式杀手功能访问能力。
 */
@Mixin(PlayerShopComponent.class)
public abstract class PlayerShopComponentMixin {
    @Redirect(
            method = "applyBlackoutCooldownToAllKillers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private static boolean sparkfactionapi$useKillerFeatureAccessCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player
    ) {
        return FactionCapabilityBridge.canUseKillerFeatureAccess(player, gameComponent);
    }
}
