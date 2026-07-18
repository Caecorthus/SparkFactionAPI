package dev.caecorthus.sparkfactionapi.mixin.compat.sparkstrength;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Guards Noisemaker's selected target before its cooldown is consumed.
 * 在消耗冷却前拦截噪音制造者选中的目标。
 */
@Pseudo
@Mixin(targets = "annina.sparkstrength.role.noisemaker.NoisemakerGlowService", remap = false)
public abstract class NoisemakerAffectMixin {
    @Inject(method = "tryUseBackpackGlow", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardNoisemakerGlow(
            ServerPlayerEntity actor,
            UUID targetUuid,
            CallbackInfo ci
    ) {
        ServerPlayerEntity target = PlayerAffectMixinGuard.onlineTarget(actor, targetUuid);
        if (target != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("sparkstrength", "noisemaker_glow")
        )) {
            ci.cancel();
        }
    }
}
