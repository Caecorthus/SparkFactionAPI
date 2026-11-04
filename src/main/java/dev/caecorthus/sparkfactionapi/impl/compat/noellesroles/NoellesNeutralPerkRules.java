package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeFactionRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import org.jetbrains.annotations.Nullable;

/**
 * The faction that NoellesRoles' {@code KILLER || NEUTRAL} perk gates read (contract D3). Only the two Scavenger
 * hidden-body gates use it: the client body visibility check and the client {@code CanTargetBody} listener. An
 * appended native Wathe constant whose faction is {@code neutralAligned} reads as NEUTRAL there, so its roles keep the
 * perk they had while they were disguised as NEUTRAL. Every built-in constant, and every other appended constant, is
 * returned unchanged. Allocation-free; safe on the render thread.
 * NoellesRoles 中 {@code KILLER || NEUTRAL} 福利判定读取的阵营（契约 D3）。只用于清道夫隐藏尸体的两处判定：客户端
 * 尸体可见性与客户端 {@code CanTargetBody} 监听器。定义为 {@code neutralAligned} 的追加原生 Wathe 常量在此读作
 * NEUTRAL，使其角色保留伪装成 NEUTRAL 时的福利。所有内置常量及其它追加常量原样返回。不分配对象，可在渲染线程调用。
 */
public final class NoellesNeutralPerkRules {
    private NoellesNeutralPerkRules() {
    }

    /**
     * NEUTRAL for an appended native constant that {@code SparkFactionApi.isNeutralAligned} accepts; the given faction
     * otherwise (NONE, CIVILIAN, KILLER and NEUTRAL included).
     * {@code SparkFactionApi.isNeutralAligned} 认可的追加原生常量返回 NEUTRAL；其余（含 NONE、CIVILIAN、KILLER、NEUTRAL）原样返回。
     */
    public static @Nullable Faction neutralPerkFaction(@Nullable Faction faction) {
        return faction != Faction.NEUTRAL && NativeFactionRules.isNeutralAlignedFaction(faction) ? Faction.NEUTRAL : faction;
    }

    /**
     * Drop-in replacement for {@code role.getFaction()} inside NoellesRoles' merged Scavenger body-hide handler (stack
     * shape {@code Role -> Faction}, same null behavior). Its name and descriptor are written into that handler by
     * {@link NoellesScavengerBodyHideRewrite}; keep them stable.
     * 在 NoellesRoles 合并后的清道夫尸体隐藏处理器中替换 {@code role.getFaction()}（栈形状 {@code Role -> Faction}，空值行为相同）。
     * {@link NoellesScavengerBodyHideRewrite} 会把这个名称与描述符写进该处理器，请保持稳定。
     */
    public static @Nullable Faction neutralPerkFactionOf(Role role) {
        return neutralPerkFaction(role.getFaction());
    }
}
