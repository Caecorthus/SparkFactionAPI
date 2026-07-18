package dev.caecorthus.sparkfactionapi.mixin.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Makes denied players count as outside poison gas so its ordinary cleanup still runs.
 * 将被拒绝影响的玩家视为不在毒气内，以保留正常的效果清理流程。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.entity.PoisonGasCloudEntity", remap = false)
public abstract class NoellesRolesPoisonGasAffectMixin {
    @Shadow
    private UUID ownerUuid;

    @Inject(method = "isInGas", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void sparkfactionapi$guardPoisonGas(
            ServerPlayerEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity cloud = (Entity) (Object) this;
        if (cloud.getWorld().getServer() == null) {
            return;
        }
        ServerPlayerEntity actor = cloud.getWorld().getServer().getPlayerManager().getPlayer(ownerUuid);
        if (actor != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("noellesroles", "poison_gas")
        )) {
            cir.setReturnValue(false);
        }
    }
}
