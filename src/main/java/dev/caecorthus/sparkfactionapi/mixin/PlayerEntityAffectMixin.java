package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards ordinary entity use before entity callbacks or held-item consumption.
 * 在实体回调或手持物品消耗前拦截普通实体交互。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAffectMixin {
    private static final Identifier USE_ENTITY = Identifier.of("minecraft", "use_entity");

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardPlayerInteraction(
            Entity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity actor) || !(entity instanceof PlayerEntity target)) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(actor.getWorld());
        if (!SparkFactionApi.canAffectPlayer(actor, target, USE_ENTITY, gameComponent)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
