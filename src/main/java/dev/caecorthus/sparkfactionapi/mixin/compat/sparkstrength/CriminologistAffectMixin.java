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
 * Guards Criminologist suspect selection before coins or cooldown are consumed.
 * 在消耗金币或冷却前拦截犯罪学家的嫌疑人选择。
 */
@Pseudo
@Mixin(targets = "annina.sparkstrength.role.detective.CriminologistService", remap = false)
public abstract class CriminologistAffectMixin {
    @Inject(method = "handleSelection", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardCriminologistSelection(
            ServerPlayerEntity actor,
            UUID victimUuid,
            UUID suspectUuid,
            CallbackInfo ci
    ) {
        ServerPlayerEntity target = PlayerAffectMixinGuard.onlineTarget(actor, suspectUuid);
        if (target != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("sparkstrength", "criminologist_track")
        )) {
            ci.cancel();
        }
    }
}
