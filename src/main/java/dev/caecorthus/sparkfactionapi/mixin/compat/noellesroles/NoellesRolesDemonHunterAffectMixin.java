package dev.caecorthus.sparkfactionapi.mixin.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.mixin.compat.PlayerAffectMixinGuard;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards the Demon Hunter shot before trigger sound, ammunition, records, or damage.
 * 在扳机音效、弹药、记录或伤害产生前拦截恶魔猎人的射击。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.demonhunter.DemonHunterShootC2SPacket$Receiver", remap = false)
public abstract class NoellesRolesDemonHunterAffectMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void sparkfactionapi$guardDemonHunterShot(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity actor = context.player();
        ServerPlayerEntity target = PlayerAffectMixinGuard.entityTarget(
                actor,
                PlayerAffectMixinGuard.intAccessor(payload, "target")
        );
        if (target != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("noellesroles", "demon_hunter_shot")
        )) {
            ci.cancel();
        }
    }
}
