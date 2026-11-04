package dev.caecorthus.sparkfactionapi.client.mixin.compat.noellesroles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesNeutralPerkRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps NoellesRoles' Scavenger hidden-body targeting for neutral-aligned native factions (contract D3). NoellesRoles'
 * client {@code CanTargetBody} listener lets only KILLER or NEUTRAL viewers target a hidden body; its one
 * {@code Role#getFaction()} read is passed through {@link NoellesNeutralPerkRules#neutralPerkFaction}, so the
 * witch faction and Team Jiahao read as NEUTRAL there and nothing else changes. The listener is the javac lambda
 * {@code lambda$onInitializeClient$6(Entity, PlayerBodyEntity)Z} of the NoellesRoles 1.7.6-h1.5.6-spark jar that
 * SparkWitch ships (sha256 fcb0da69...). It is selected by name only because its descriptor holds a Minecraft class
 * that is intermediary in production but Yarn-named in Loom dev runs; the local seam test pins the descriptor and the
 * call shape. {@code require = 1} fails loading on a renumbered lambda instead of silently dropping the perk;
 * {@code @Pseudo} skips the mixin when NoellesRoles is absent.
 * 为中立同盟的原生阵营保留 NoellesRoles 清道夫隐藏尸体的选中能力（契约 D3）。NoellesRoles 的客户端 {@code CanTargetBody}
 * 监听器只允许 KILLER 或 NEUTRAL 选中隐藏尸体；其唯一一次 {@code Role#getFaction()} 读取经过
 * {@link NoellesNeutralPerkRules#neutralPerkFaction}，因此魔女阵营与嘉豪阵营在此读作 NEUTRAL，其余不变。该监听器是
 * SparkWitch 随附 NoellesRoles 1.7.6-h1.5.6-spark（sha256 fcb0da69...）中的 javac lambda
 * {@code lambda$onInitializeClient$6(Entity, PlayerBodyEntity)Z}。只按名称选择，因为描述符含 Minecraft 类，
 * 生产环境为 intermediary 名而 Loom 开发环境为 Yarn 名；本地接缝测试固定描述符与调用形状。lambda 重新编号时
 * {@code require = 1} 让加载失败，而不是静默丢失福利；未安装 NoellesRoles 时 {@code @Pseudo} 跳过此 mixin。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.client.NoellesrolesClient", remap = false)
public abstract class NoellesScavengerBodyTargetNeutralPerkMixin {
    @WrapOperation(
            method = "lambda$onInitializeClient$6",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/Role;getFaction()Ldev/doctor4t/wathe/api/Faction;",
                    remap = false
            ),
            require = 1,
            allow = 1,
            remap = false
    )
    private static Faction sparkfactionapi$readNeutralAlignedAsNeutral(Role role, Operation<Faction> original) {
        return NoellesNeutralPerkRules.neutralPerkFaction(original.call(role));
    }
}
