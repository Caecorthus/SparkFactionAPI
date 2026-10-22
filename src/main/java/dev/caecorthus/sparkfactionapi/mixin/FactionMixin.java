package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents SparkFactionAPI roles from leaking into Wathe's native buckets.
 * 防止 SparkFactionAPI 角色被 wathe 原生阵营桶误判。
 */
@Mixin(Faction.class)
public abstract class FactionMixin {
    @Inject(method = "fromRole", at = @At("HEAD"), cancellable = true)
    private static void sparkfactionapi$overrideSparkFactionRole(
            Role role,
            CallbackInfoReturnable<Faction> cir
    ) {
        // Null-returning lookup avoids an Optional and a bound method ref on every fromRole call.
        // 返回 null 的查询避免每次 fromRole 调用都分配 Optional 与绑定方法引用。
        Faction override = FactionRegistryImpl.nativeFactionOverrideOrNull(role);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
