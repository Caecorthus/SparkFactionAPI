package dev.caecorthus.sparkfactionapi.mixin.compat.noellesroles;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Makes denied players count as outside poison gas so its ordinary cleanup still runs.
 * 将被拒绝影响的玩家视为不在毒气内，以保留正常的效果清理流程。
 * {@code isInGas} is a pure geometry check run per player every tick, so the owner lookup and affect
 * policies run only for players actually inside the gas.
 * {@code isInGas} 是每 tick 对每名玩家执行的纯几何判定，因此只对确实处于毒气中的玩家查询施放者并评估影响策略。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.entity.PoisonGasCloudEntity", remap = false)
public abstract class NoellesRolesPoisonGasAffectMixin {
    private static final Identifier ACTION = Identifier.of("noellesroles", "poison_gas");

    @Shadow
    private UUID ownerUuid;

    @ModifyReturnValue(method = "isInGas", at = @At("RETURN"), require = 0, remap = false)
    private boolean sparkfactionapi$guardPoisonGas(boolean inGas, ServerPlayerEntity target) {
        if (!inGas) {
            return false;
        }
        Entity cloud = (Entity) (Object) this;
        if (cloud.getWorld().getServer() == null) {
            return true;
        }
        ServerPlayerEntity actor = cloud.getWorld().getServer().getPlayerManager().getPlayer(ownerUuid);
        return actor == null || PlayerAffectMixinGuard.allows(actor, target, ACTION);
    }
}
