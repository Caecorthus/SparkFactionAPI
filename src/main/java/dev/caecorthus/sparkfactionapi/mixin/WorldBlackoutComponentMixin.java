package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes blackout immunity/night vision through killer-feature access.
 * 将黑灯免疫/夜视改为显式杀手功能访问能力。
 */
@Mixin(WorldBlackoutComponent.class)
public abstract class WorldBlackoutComponentMixin {
    @Redirect(
            method = "applyBlackoutEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparkfactionapi$useKillerFeatureAccessCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player
    ) {
        return FactionCapabilityBridge.canUseKillerFeatureAccess(player, gameComponent);
    }
}
