package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents SparkFactionAPI roles from leaking into Wathe's native buckets: roles registered with a NONE bucket read
 * as NEUTRAL, or as their native faction's appended constant when the faction is declared native; roles bound with
 * {@code bindRoleFaction} to a native faction read as its constant too. Everyone else keeps Wathe's own answer.
 * 防止 SparkFactionAPI 角色被 wathe 原生阵营桶误判：以 NONE 桶注册的角色读作 NEUTRAL，若其阵营声明为原生阵营
 * 则读作该阵营追加的常量；通过 {@code bindRoleFaction} 绑定到原生阵营的角色同样读作其常量。其余角色保持 Wathe 原有结果。
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
