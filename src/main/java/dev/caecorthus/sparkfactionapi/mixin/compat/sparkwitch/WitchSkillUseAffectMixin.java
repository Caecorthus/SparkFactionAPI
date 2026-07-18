package dev.caecorthus.sparkfactionapi.mixin.compat.sparkwitch;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * Guards targeted witch skills before the skill body and cooldown policy run.
 * 在目标型魔女技能主体与冷却策略运行前拦截使用。
 */
@Pseudo
@Mixin(targets = "dev.caecorthus.sparkwitch.skill.WitchSkillUseService", remap = false)
public abstract class WitchSkillUseAffectMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void sparkfactionapi$guardTargetedWitchSkill(
            ServerPlayerEntity actor,
            Optional<UUID> targetUuid,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayerEntity target = targetUuid
                .map(uuid -> PlayerAffectMixinGuard.onlineTarget(actor, uuid))
                .orElse(null);
        if (target != null && !PlayerAffectMixinGuard.allows(
                actor,
                target,
                Identifier.of("sparkwitch", "witch_skill")
        )) {
            cir.setReturnValue(false);
        }
    }
}
