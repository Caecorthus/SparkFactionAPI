package dev.caecorthus.sparkfactionapi.mixin.nativefaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeFactionRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets native factions built with {@code dealtInWatheNeutralPool(true)} take part in Wathe's neutral pass.
 * Wathe 1.5.6 {@code assignNeutrals} reads {@code role.getFaction() == Faction.NEUTRAL} exactly twice, both directly
 * in the method body: the already-assigned neutral count and the candidate filter. Only those two reads see such
 * roles as NEUTRAL (pinned with require = allow = 2, so a changed Wathe body fails loudly); every other
 * {@code getFaction()} call, including the killer and civilian passes, keeps the real constant.
 * 让 {@code dealtInWatheNeutralPool(true)} 的原生阵营参与 Wathe 中立分配。Wathe 1.5.6 的 {@code assignNeutrals}
 * 恰好两次直接在方法体内读取 {@code role.getFaction() == Faction.NEUTRAL}：已分配中立计数与候选过滤。只有这两次
 * 读取把此类角色视为 NEUTRAL（以 require = allow = 2 固定，Wathe 方法体变化会直接报错）；其它所有
 * {@code getFaction()} 调用（包括杀手与平民分配）仍得到真实常量。
 */
@Mixin(value = ScoreboardRoleSelectorComponent.class, remap = false)
public abstract class ScoreboardRoleSelectorNeutralPoolMixin {
    @WrapOperation(
            method = "assignNeutrals",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/Role;getFaction()Ldev/doctor4t/wathe/api/Faction;"
            ),
            require = 2,
            allow = 2
    )
    private Faction sparkfactionapi$neutralPoolFaction(Role role, Operation<Faction> original) {
        return NativeFactionRules.neutralPoolView(original.call(role));
    }
}
