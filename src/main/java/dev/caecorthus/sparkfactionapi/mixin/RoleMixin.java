package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Role;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors the faction override for Role#isNeutral: NEUTRAL-shielded roles are neutral, roles shown as a native custom
 * constant are not (use {@code SparkFactionApi.isNeutralAligned} for perks they keep).
 * 与阵营 override 保持一致：受 NEUTRAL 保护的角色为中立，显示为原生自定义常量的角色不是中立（需要保留的中立特权请用
 * {@code SparkFactionApi.isNeutralAligned}）。
 */
@Mixin(Role.class)
public abstract class RoleMixin {
    @Inject(method = "isNeutral", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$overrideNeutralStatus(CallbackInfoReturnable<Boolean> cir) {
        Boolean override = FactionRegistryImpl.nativeNeutralOverrideOrNull((Role) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
