package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards location-specific entity use before the target receives the callback.
 * 在目标收到指定位置实体交互回调前完成拦截。
 */
@Mixin(Entity.class)
public abstract class EntityAffectMixin {
    private static final Identifier USE_ENTITY = Identifier.of("minecraft", "use_entity");

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardPlayerInteractionAt(
            PlayerEntity player,
            Vec3d hitPos,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        Entity self = (Entity) (Object) this;
        if (!(player instanceof ServerPlayerEntity actor) || !(self instanceof PlayerEntity target)) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(actor.getWorld());
        if (!SparkFactionApi.canAffectPlayer(actor, target, USE_ENTITY, gameComponent)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
