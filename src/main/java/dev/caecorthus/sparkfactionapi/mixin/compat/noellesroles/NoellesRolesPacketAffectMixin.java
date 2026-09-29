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
 * Each receiver is a javac-numbered lambda in {@code registerPackets}, pinned by name and payload
 * descriptor to the NoellesRoles 1.7.6-h1.5.6-spark jar that SparkWitch ships (sha256 fcb0da69...).
 * Lambda numbers change between builds, so {@code require = 1} fails loading on a stale selector
 * instead of silently dropping the guard; {@code @Pseudo} still skips this mixin when NoellesRoles is absent.
 * 每个接收器都是 {@code registerPackets} 中由 javac 编号的 lambda，按名称和载荷描述符固定到
 * SparkWitch 随附的 NoellesRoles 1.7.6-h1.5.6-spark（sha256 fcb0da69...）。编号会随构建变化，
 * 因此 {@code require = 1} 让过期选择器在加载时报错，而不是静默丢失拦截；未安装 NoellesRoles 时
 * {@code @Pseudo} 仍会跳过此 mixin。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.Noellesroles", remap = false)
public abstract class NoellesRolesPacketAffectMixin {
    @Inject(
            method = "lambda$registerPackets$0(Lorg/agmas/noellesroles/packet/MorphC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardMorph(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "player", Identifier.of("noellesroles", "morph"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$4(Lorg/agmas/noellesroles/packet/SwapperC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
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

    @Inject(
            method = "lambda$registerPackets$6(Lorg/agmas/noellesroles/packet/AssassinGuessRoleC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardAssassin(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "assassin"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$7(Lorg/agmas/noellesroles/packet/ReporterMarkC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardReporter(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "reporter"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$8(Lorg/agmas/noellesroles/packet/DetectiveInvestigateC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardDetective(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "detective"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$9(Lorg/agmas/noellesroles/packet/TaotieSwallowC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardTaotie(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "taotie"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$10(Lorg/agmas/noellesroles/packet/ShadowAllyRequestC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardShadowAlly(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "shadow_jester"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$12(Lorg/agmas/noellesroles/packet/SilencerSilenceC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void sparkfactionapi$guardSilencer(
            @Coerce Object payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        cancelIfDenied(payload, context, "targetPlayer", Identifier.of("noellesroles", "silencer"), ci);
    }

    @Inject(
            method = "lambda$registerPackets$13(Lorg/agmas/noellesroles/packet/PartyAnimalBuzzC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
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
