package dev.caecorthus.sparkfactionapi.mixin.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
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
 * Guards NoellesRoles' UUID-targeted packets before cooldowns or effects.
 * 在冷却与效果发生前拦截 NoellesRoles 的 UUID 定向数据包。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.Noellesroles", remap = false)
public abstract class NoellesRolesPacketAffectMixin {
    @Inject(method = "lambda$registerPackets$0", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardMorphOrVoodoo(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "player", Identifier.of("noellesroles", "morph"), ci);
    }

    @Inject(method = "lambda$registerPackets$4", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardSwapper(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        Identifier actionId = Identifier.of("noellesroles", "swapper");
        cancelIfDenied(payload, context, "player", actionId, ci);
        if (!ci.isCancelled()) {
            cancelIfDenied(payload, context, "player2", actionId, ci);
        }
    }

    @Inject(method = "lambda$registerPackets$6", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardAssassin(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "assassin"), ci);
    }

    @Inject(method = "lambda$registerPackets$7", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardReporter(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "reporter"), ci);
    }

    @Inject(method = "lambda$registerPackets$8", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardDetective(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "detective"), ci);
    }

    @Inject(method = "lambda$registerPackets$9", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardTaotie(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "taotie"), ci);
    }

    @Inject(method = "lambda$registerPackets$10", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardShadowAlly(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "shadow_jester"), ci);
    }

    @Inject(method = "lambda$registerPackets$12", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardSilencer(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "silencer"), ci);
    }

    @Inject(method = "lambda$registerPackets$13", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardPartyAnimal(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "party_animal"), ci);
    }

    private static void cancelIfDenied(
            Object payload,
            ServerPlayNetworking.Context context,
            String accessor,
            Identifier actionId,
            CallbackInfo ci
    ) {
        ServerPlayerEntity actor = context.player();
        ServerPlayerEntity target = PlayerAffectMixinGuard.onlineTarget(
                actor,
                PlayerAffectMixinGuard.uuidAccessor(payload, accessor)
        );
        if (target != null && !PlayerAffectMixinGuard.allows(actor, target, actionId)) {
            ci.cancel();
        }
    }
}
