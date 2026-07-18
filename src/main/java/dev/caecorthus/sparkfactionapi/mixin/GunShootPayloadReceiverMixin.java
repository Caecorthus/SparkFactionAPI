package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards Wathe gun hits before ammunition state, sounds, records, kills, or cooldowns.
 * 在弹药状态、音效、记录、击杀或冷却生效前拦截 Wathe 枪击。
 */
@Mixin(value = GunShootPayload.Receiver.class, remap = false)
public abstract class GunShootPayloadReceiverMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$guardGunShot(
            GunShootPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity actor = context.player();
        ItemStack mainHandStack = actor.getMainHandStack();
        if (actor.isSpectator()
                || !mainHandStack.isIn(WatheItemTags.GUNS)
                || actor.getItemCooldownManager().isCoolingDown(mainHandStack.getItem())
                || (mainHandStack.isOf(WatheItems.DERRINGER)
                && Boolean.TRUE.equals(mainHandStack.get(WatheDataComponentTypes.USED)))) {
            return;
        }
        if (!(actor.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity target)
                || target.isSpectator()
                || target.distanceTo(actor) >= 65.0) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(actor.getWorld());
        if (!SparkFactionApi.canAffectPlayer(
                actor,
                target,
                GameConstants.DeathReasons.GUN,
                gameComponent
        )) {
            ci.cancel();
        }
    }
}
