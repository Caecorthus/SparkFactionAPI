package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Role;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors the faction override for Role#isNeutral.
 * 与阵营 override 保持一致，避免自定义角色被当作原生中立。
 */
@Mixin(Role.class)
public abstract class RoleMixin {
    @Inject(method = "isNeutral", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$overrideNeutralStatus(CallbackInfoReturnable<Boolean> cir) {
        FactionRegistryImpl.nativeNeutralOverride((Role) (Object) this).ifPresent(cir::setReturnValue);
    }
}
