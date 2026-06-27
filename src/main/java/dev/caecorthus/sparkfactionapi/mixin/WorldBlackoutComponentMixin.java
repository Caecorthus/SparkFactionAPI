package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes blackout night vision through the explicit blackout immunity capability.
 * 将黑灯夜视改为显式熄灯免疫能力。
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
    private boolean sparkfactionapi$useBlackoutImmunityCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player
    ) {
        return FactionCapabilityBridge.hasBlackoutImmunity(player, gameComponent);
    }
}
