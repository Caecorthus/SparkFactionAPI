package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards Wathe knife stabs before veteran charges, records, kills, or cooldowns.
 * 在老兵次数、记录、击杀或冷却生效前拦截 Wathe 匕首刺杀。
 */
@Mixin(value = KnifeStabPayload.Receiver.class, remap = false)
public abstract class KnifeStabPayloadReceiverMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardKnifeStab(
            KnifeStabPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity actor = context.player();
        if (actor.isSpectator()) {
            return;
        }
        if (!(actor.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity target)
                || target.isSpectator()
                || target.distanceTo(actor) > 3.0) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(actor.getWorld());
        if (!SparkFactionApi.canAffectPlayer(
                actor,
                target,
                GameConstants.DeathReasons.KNIFE,
                gameComponent
        )) {
            ci.cancel();
        }
    }
}
