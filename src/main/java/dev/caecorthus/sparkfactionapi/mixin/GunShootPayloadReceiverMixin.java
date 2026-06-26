package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Splits gun punishment identity into shooter and victim capabilities.
 * 将枪罚身份拆成开枪者与受击者两个显式能力。
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class GunShootPayloadReceiverMixin {
    @Redirect(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 0
            )
    )
    private boolean sparkfactionapi$useVictimGunPunishmentCapability(
            GameWorldComponent gameComponent,
            PlayerEntity victim
    ) {
        return FactionCapabilityBridge.isPunishableInnocentGunVictim(victim, gameComponent);
    }

    @Redirect(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 1
            )
    )
    private boolean sparkfactionapi$useShooterBackfireCapability(
            GameWorldComponent gameComponent,
            PlayerEntity shooter
    ) {
        return FactionCapabilityBridge.isPunishableInnocentGunShooter(shooter, gameComponent);
    }

    @Redirect(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 2
            )
    )
    private boolean sparkfactionapi$useShooterMoodPenaltyCapability(
            GameWorldComponent gameComponent,
            PlayerEntity shooter
    ) {
        return FactionCapabilityBridge.isPunishableInnocentGunShooter(shooter, gameComponent);
    }

    @Redirect(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 3
            )
    )
    private boolean sparkfactionapi$useShooterCooldownCapability(
            GameWorldComponent gameComponent,
            PlayerEntity shooter
    ) {
        return FactionCapabilityBridge.isPunishableInnocentGunShooter(shooter, gameComponent);
    }

    @Redirect(
            method = "lambda$receive$2",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private static boolean sparkfactionapi$useScheduledShooterPunishmentCapability(
            GameWorldComponent gameComponent,
            PlayerEntity shooter
    ) {
        return FactionCapabilityBridge.isPunishableInnocentGunShooter(shooter, gameComponent);
    }
}
