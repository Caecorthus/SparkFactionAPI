package dev.caecorthus.sparkfactionapi.api;

import dev.caecorthus.sparkfactionapi.impl.collision.EntityCollisionExemptions;
import dev.caecorthus.sparkfactionapi.impl.economy.FactionEconomyRules;
import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentRules;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeFactionRules;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactions;
import dev.caecorthus.sparkfactionapi.impl.registry.EffectiveFactionResolvers;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionCatalog;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionRegistryBootstrap;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionRoleCatalog;
import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectRules;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionInstinctRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class SparkFactionApi {
    private SparkFactionApi() {
    }

    public static void bootstrap() {
        FactionRegistryBootstrap.bootstrap();
    }

    public static FactionDefinition registerFaction(FactionDefinition definition) {
        return FactionCatalog.registerFaction(definition);
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        return FactionRoleCatalog.registerRole(definition);
    }

    public static Identifier resolveBaseFaction(Role role) {
        return FactionRoleCatalog.resolveBaseFaction(role);
    }

    /**
     * Faction id of a Wathe {@code Faction} constant: built-ins give {@code wathe:*}, constants appended for a native
     * faction declared under {@code sparkfactionapi:native_wathe_factions} in a fabric.mod.json give their declared
     * id. Empty for null or for a constant SparkFactionAPI did not append. Match records carry {@code Faction.name()};
     * map them with {@code factionIdOf(Faction.valueOf(name))} (valueOf throws for a name this side lacks).
     * Wathe {@code Faction} 常量对应的阵营 id：内置常量给出 {@code wathe:*}，为 fabric.mod.json 中
     * {@code sparkfactionapi:native_wathe_factions} 声明的原生阵营追加的常量给出声明的 id；null 或非 SparkFactionAPI
     * 追加的常量返回空。对局记录保存 {@code Faction.name()}，可用 {@code factionIdOf(Faction.valueOf(name))} 映射
     * （本端没有该名字时 valueOf 会抛出异常）。
     *
     * @since 0.1.6.0
     */
    public static Optional<Identifier> factionIdOf(Faction faction) {
        return NativeWatheFactions.factionId(faction);
    }

    /**
     * Binds a role SparkFactionAPI did not register (for example {@code noellesroles:corrupt_cop}) to a registered
     * custom faction. Call it during common mod initialization, after registering the faction. It is resolved lazily
     * by role id on both sides, so mod load order does not matter. Effects: {@link #resolveBaseFaction} returns the
     * faction id; when the faction is native, {@code Faction#fromRole} returns its constant and {@code Role#isNeutral}
     * returns false. Binding the same pair again is a no-op.
     * 把非 SparkFactionAPI 注册的角色（例如 {@code noellesroles:corrupt_cop}）绑定到已注册的自定义阵营。应在通用
     * 模组初始化期间、注册该阵营之后调用。双端都按角色 id 惰性解析，与模组加载顺序无关。效果：
     * {@link #resolveBaseFaction} 返回该阵营 id；若该阵营为原生阵营，{@code Faction#fromRole} 返回其常量，
     * {@code Role#isNeutral} 返回 false。重复绑定同一对不产生变化。
     *
     * @throws IllegalArgumentException the faction is not a registered custom faction, or the role is registered
     *                                  through SparkFactionAPI 阵营不是已注册自定义阵营，或该角色由 SparkFactionAPI 注册
     * @throws IllegalStateException    the role is already bound to a different faction 该角色已绑定到其它阵营
     * @since 0.1.6.0
     */
    public static void bindRoleFaction(Identifier roleId, Identifier factionId) {
        FactionRoleCatalog.bindRoleFaction(roleId, factionId);
    }

    /**
     * True when the role's Wathe faction is NEUTRAL, or is the constant of a native faction whose definition has
     * {@code neutralAligned(true)}. Use it instead of {@code Role#isNeutral} for "neutral perks" that native factions
     * keep. False for null.
     * 角色的 Wathe 阵营为 NEUTRAL，或为定义了 {@code neutralAligned(true)} 的原生阵营常量时为 true。对需要保留给
     * 原生阵营的“中立特权”，请用它代替 {@code Role#isNeutral}。null 返回 false。
     *
     * @since 0.1.6.0
     */
    public static boolean isNeutralAligned(Role role) {
        return NativeFactionRules.isNeutralAligned(role);
    }

    public static Identifier resolveEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        return EffectiveFactionResolvers.resolve(player, gameComponent);
    }

    /**
     * @deprecated A role alone has no player context for registered effective-faction resolvers.
     *             Use {@link #resolveBaseFaction(Role)} or the player overload instead.
     *             单独的角色没有玩家上下文，无法执行有效阵营解析链；请改用基础阵营方法或玩家重载。
     */
    @Deprecated(forRemoval = false)
    public static Identifier resolveEffectiveFaction(Role role) {
        return FactionRoleCatalog.resolveBaseFaction(role);
    }

    public static boolean canAffectPlayer(
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    ) {
        // Bootstraps the registry before any policy runs, as the retired FactionRegistryImpl facade did.
        // 与已退役的 FactionRegistryImpl 门面一致：在执行任何策略之前先完成注册表初始化。
        FactionRegistryBootstrap.bootstrap();
        return PlayerAffectRules.canAffectPlayer(actor, target, actionId, gameComponent);
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        return FactionCatalog.capabilities(factionId);
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        return FactionCatalog.getFaction(factionId);
    }

    public static void registerEffectiveFactionResolver(EffectiveFactionResolver resolver) {
        EffectiveFactionResolvers.register(resolver);
    }

    public static void registerPlayerAffectPolicy(PlayerAffectPolicy policy) {
        PlayerAffectRules.register(policy);
    }

    public static void registerEconomyPolicy(FactionEconomyPolicy policy) {
        FactionEconomyRules.register(policy);
    }

    public static void registerGunPunishmentPolicy(FactionGunPunishmentPolicy policy) {
        FactionGunPunishmentRules.register(policy);
    }

    public static void registerInstinctPolicy(FactionInstinctPolicy policy) {
        FactionInstinctRules.register(policy);
    }

    /**
     * Registers a collision exemption during mod initialization. If either participant matches,
     * pairwise {@code Entity.collidesWith(Entity)} returns false and the corresponding
     * {@code LivingEntity.pushAway(Entity)} fallback is cancelled. Block collision, projectile
     * targeting, and vehicle-specific collision overrides are outside this contract.
     * 应在模组初始化期间注册碰撞豁免；任一参与实体匹配时，双方
     * {@code Entity.collidesWith(Entity)} 都会返回 false，并取消对应的
     * {@code LivingEntity.pushAway(Entity)} 兜底推动。本契约不包含方块碰撞、投射物选取或载具碰撞覆写。
     *
     * @param exemption non-null exemption predicate
     */
    public static void registerEntityCollisionExemption(EntityCollisionExemption exemption) {
        EntityCollisionExemptions.register(exemption);
    }
}
