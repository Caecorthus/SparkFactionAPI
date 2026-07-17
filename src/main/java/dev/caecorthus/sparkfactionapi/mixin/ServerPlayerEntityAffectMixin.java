package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards server player attacks and player-attributed damage before effects.
 * 在效果生效前拦截服务端玩家攻击与可归因到玩家的伤害。
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityAffectMixin {
    private static final Identifier VANILLA_ATTACK = Identifier.of("minecraft", "attack");
    private static final Identifier PLAYER_DAMAGE = Identifier.of("minecraft", "damage");

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardPlayerAttack(Entity entity, CallbackInfo ci) {
        ServerPlayerEntity actor = (ServerPlayerEntity) (Object) this;
        if (actor.isSpectator() || !(entity instanceof PlayerEntity target)) {
            return;
        }

        Identifier actionId = actor.getMainHandStack().isOf(WatheItems.BAT)
                ? GameConstants.DeathReasons.BAT
                : VANILLA_ATTACK;
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(actor.getWorld());
        if (!SparkFactionApi.canAffectPlayer(actor, target, actionId, gameComponent)) {
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardPlayerDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayerEntity target = (ServerPlayerEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity actor)) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(target.getWorld());
        if (!SparkFactionApi.canAffectPlayer(actor, target, PLAYER_DAMAGE, gameComponent)) {
            cir.setReturnValue(false);
        }
    }
}
