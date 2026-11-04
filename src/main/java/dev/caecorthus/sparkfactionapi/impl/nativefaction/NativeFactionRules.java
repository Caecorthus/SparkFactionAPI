package dev.caecorthus.sparkfactionapi.impl.nativefaction;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionCatalog;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the per-faction flags of native Wathe factions ({@code neutralAligned}, {@code dealtInWatheNeutralPool}).
 * Both flags are read from the registered {@link FactionDefinition} of an appended constant, so a declared but
 * unregistered native faction behaves as if both were false.
 * 持有原生 Wathe 阵营的阵营级标记（{@code neutralAligned}、{@code dealtInWatheNeutralPool}）。两者都读取追加常量
 * 对应的已注册 {@link FactionDefinition}，因此已声明但未注册的原生阵营视为两者皆为 false。
 */
public final class NativeFactionRules {
    private NativeFactionRules() {
    }

    /**
     * True when the role's Wathe faction is NEUTRAL, or is a native custom constant whose definition is
     * {@code neutralAligned}. A role registered under a native faction with an explicit CIVILIAN or KILLER Wathe
     * bucket is not neutral-aligned, exactly as it was not neutral before. Allocation-free.
     * 角色的 Wathe 阵营为 NEUTRAL，或为定义了 {@code neutralAligned} 的原生自定义常量时为 true。在原生阵营下但显式
     * 指定 CIVILIAN 或 KILLER 原生桶的角色不算中立同盟，与以前它不是中立一致。不分配对象。
     */
    public static boolean isNeutralAligned(@Nullable Role role) {
        return role != null && isNeutralAlignedFaction(role.getFaction());
    }

    /**
     * Faction-level half of {@link #isNeutralAligned(Role)}: NEUTRAL, or a native constant whose definition is
     * {@code neutralAligned}.
     * {@link #isNeutralAligned(Role)} 的阵营部分：NEUTRAL，或定义了 {@code neutralAligned} 的原生常量。
     */
    public static boolean isNeutralAlignedFaction(@Nullable Faction faction) {
        if (faction == Faction.NEUTRAL) {
            return true;
        }
        FactionDefinition definition = nativeDefinitionOrNull(faction);
        return definition != null && definition.neutralAligned();
    }

    /**
     * What Wathe's neutral pass sees: NEUTRAL for a native constant whose definition is
     * {@code dealtInWatheNeutralPool}, the given faction otherwise.
     * Wathe 中立分配看到的阵营：定义了 {@code dealtInWatheNeutralPool} 的原生常量视为 NEUTRAL，其余原样返回。
     */
    public static Faction neutralPoolView(Faction faction) {
        FactionDefinition definition = nativeDefinitionOrNull(faction);
        return definition != null && definition.dealtInWatheNeutralPool() ? Faction.NEUTRAL : faction;
    }

    /**
     * Registration-time check: {@code dealtInWatheNeutralPool} on a faction that is not a declared native faction is
     * an error (it could never take effect); {@code neutralAligned} there is only meaningless, so it is reported by
     * the caller as a warning.
     * 注册时检查：非已声明原生阵营使用 {@code dealtInWatheNeutralPool} 视为错误（永远不会生效）；
     * {@code neutralAligned} 在那里只是无意义，由调用方记录警告。
     *
     * @return true when {@code neutralAligned} is set on a faction that is not native 非原生阵营设置了 neutralAligned 时返回 true
     */
    public static boolean validateNativeFlags(FactionDefinition definition) {
        boolean nativeFaction = NativeWatheFactions.isNativeCustomFaction(definition.id());
        if (definition.dealtInWatheNeutralPool() && !nativeFaction) {
            throw new IllegalArgumentException("dealtInWatheNeutralPool(true) needs a native Wathe faction declared under "
                    + NativeWatheFactionDeclarations.METADATA_KEY + " in fabric.mod.json: " + definition.id());
        }
        return definition.neutralAligned() && !nativeFaction;
    }

    private static @Nullable FactionDefinition nativeDefinitionOrNull(@Nullable Faction faction) {
        Identifier factionId = NativeWatheFactions.customFactionIdOrNull(faction);
        return factionId == null ? null : FactionCatalog.definitionOrNull(factionId);
    }
}
