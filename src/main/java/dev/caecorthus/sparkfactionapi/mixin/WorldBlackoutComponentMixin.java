package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Routes blackout night vision through the explicit blackout immunity capability.
 * 将黑灯夜视改为显式熄灯免疫能力。
 *
 * <p>A {@code @WrapOperation} that never calls the original: the answer is the same as the former {@code @Redirect},
 * but the call site stays chainable, so another mod's injector on the same Wathe killer check still applies.
 * 使用从不调用原方法的 {@code @WrapOperation}：结果与原先的 {@code @Redirect} 相同，但调用点仍可链式叠加，
 * 其他模组在同一处 Wathe 杀手判断上的注入器仍能生效。
 */
@Mixin(WorldBlackoutComponent.class)
public abstract class WorldBlackoutComponentMixin {
    @WrapOperation(
            method = "applyBlackoutEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparkfactionapi$useBlackoutImmunityCapability(
            GameWorldComponent gameComponent,
            PlayerEntity player,
            Operation<Boolean> original
    ) {
        return FactionBlackoutRules.hasBlackoutImmunity(player, gameComponent);
    }
}
