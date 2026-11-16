package dev.caecorthus.sparkfactionapi.api;

import dev.caecorthus.sparkfactionapi.component.SparkFactionThirdFactionComponent;
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
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionDraw;
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionDrawRules;
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionLineups;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionInstinctRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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

    /**
     * Category of a Wathe {@code Faction} constant: null and NONE give {@link FactionCategory#NONE}, CIVILIAN, NEUTRAL
     * and KILLER map one to one, and any other constant (the appended native factions) is
     * {@link FactionCategory#THIRD_FACTION}.
     * Wathe {@code Faction} 常量的分类：null 与 NONE 为 {@link FactionCategory#NONE}，CIVILIAN、NEUTRAL、KILLER 一一对应，
     * 其它任何常量（追加的原生阵营）为 {@link FactionCategory#THIRD_FACTION}。
     *
     * @since 2.0.0
     */
    public static FactionCategory factionCategory(@Nullable Faction faction) {
        return ThirdFactionDrawRules.categoryOf(faction);
    }

    /**
     * Category of the role's real Wathe faction ({@code role.getFaction()}, never the neutral-pool view that
     * {@code dealtInWatheNeutralPool} gives inside Wathe's neutral pass). A role of a non-native custom faction in a
     * Wathe bucket takes that bucket's category. Null gives {@link FactionCategory#NONE}.
     * 角色真实 Wathe 阵营（{@code role.getFaction()}，绝不是 {@code dealtInWatheNeutralPool} 在 Wathe 中立分配内给出的中立池
     * 视图）的分类。位于 Wathe 原生桶中的非原生自定义阵营角色取该桶的分类。null 返回 {@link FactionCategory#NONE}。
     *
     * @since 2.0.0
     */
    public static FactionCategory factionCategory(@Nullable Role role) {
        return ThirdFactionDraw.categoryOf(role);
    }

    /**
     * True for a declared native custom faction id (a {@code sparkfactionapi:native_wathe_factions} entry), whether
     * or not it is registered or has a lineup; false for null, the {@code wathe:*} built-ins and non-native factions.
     * 对已声明的原生自定义阵营 id（{@code sparkfactionapi:native_wathe_factions} 条目）返回 true，无论是否已注册或有阵容；
     * null、{@code wathe:*} 内置阵营与非原生阵营返回 false。
     *
     * @since 2.0.0
     */
    public static boolean isThirdFaction(@Nullable Identifier factionId) {
        return NativeWatheFactions.isNativeCustomFaction(factionId);
    }

    /**
     * Declared native custom faction ids in {@code Faction} ordinal order; unmodifiable, empty when none is declared.
     * 按 {@code Faction} 序号排列的已声明原生自定义阵营 id；不可修改，未声明时为空。
     *
     * @since 2.0.0
     */
    public static List<Identifier> thirdFactionIds() {
        return NativeWatheFactions.customFactionIds();
    }

    /**
     * Registers the lineup SparkFactionAPI's third-faction draw deals when this faction is the round's third faction.
     * Call it during common mod initialization, after {@link #registerFaction}. At most one third faction is drawn per
     * Wathe murder round, before Wathe's neutral pass, with a member cap of
     * {@code floor(players / thirdFactionDividend)} and only from {@link #thirdFactionMinPlayers} players on: an
     * admin-forced member makes its faction the round's faction (forced members count toward the cap), two forced
     * factions stop the draw, otherwise one drawable lineup is picked uniformly at random.
     * 注册第三方阵营抽取在本阵营成为本局第三方阵营时发放的阵容。应在通用模组初始化期间、{@link #registerFaction} 之后调用。
     * 每局 Wathe 谋杀模式最多抽出一个第三方阵营，在 Wathe 中立分配之前进行，成员上限为
     * {@code floor(总人数 / 第三方阵营除数)}，且只在人数达到 {@link #thirdFactionMinPlayers} 时进行：管理员强制的成员使其
     * 阵营成为本局阵营（强制成员计入上限），两个强制阵营会停止抽取，否则从可抽取的阵容中均匀随机抽取一个。
     *
     * @throws IllegalArgumentException the faction is not a registered, declared native faction, or its definition has
     *                                  {@code dealtInWatheNeutralPool(true)} or an {@code assignmentPolicy} (a third
     *                                  faction has exactly one natural draw path) 阵营不是已注册的已声明原生阵营，或其
     *                                  定义启用了 {@code dealtInWatheNeutralPool(true)} 或设置了 {@code assignmentPolicy}
     *                                  （第三方阵营只有一条自然抽取途径）
     * @throws IllegalStateException    a lineup is already registered for this faction 该阵营已注册过阵容
     * @since 2.0.0
     */
    public static void registerThirdFactionLineup(Identifier factionId, ThirdFactionLineup lineup) {
        ThirdFactionLineups.register(factionId, lineup);
    }

    /**
     * Server-wide minimum player count for the third-faction draw (default 18; 0 = no minimum). Saved with the
     * server scoreboard; read at each round start. SparkWitch's {@code /sparkwitch:thirdFactionMinRequirement} writes it.
     * 全服第三方阵营抽取的最低人数（默认 18；0 = 无最低人数）。随服务器计分板保存，每局开始时读取。由 SparkWitch 的
     * {@code /sparkwitch:thirdFactionMinRequirement} 写入。
     *
     * @since 2.0.0
     */
    public static int thirdFactionMinPlayers(MinecraftServer server) {
        return SparkFactionThirdFactionComponent.of(server).settings().minPlayers();
    }

    /**
     * Sets {@link #thirdFactionMinPlayers}; applies from the next round. Call it on the server thread.
     * 设置 {@link #thirdFactionMinPlayers}；从下一局生效。请在服务端线程调用。
     *
     * @throws IllegalArgumentException {@code players < 0}
     * @since 2.0.0
     */
    public static void setThirdFactionMinPlayers(MinecraftServer server, int players) {
        SparkFactionThirdFactionComponent.of(server).setMinPlayers(players);
    }

    /**
     * Server-wide third-faction dividend (default 10): member cap = {@code floor(players / dividend)}. Also set with
     * {@code /wathe:gameSettings set roleDividend third_faction <dividend>}.
     * 全服第三方阵营除数（默认 10）：成员上限 = {@code floor(总人数 / 除数)}。也可用
     * {@code /wathe:gameSettings set roleDividend third_faction <dividend>} 设置。
     *
     * @since 2.0.0
     */
    public static int thirdFactionDividend(MinecraftServer server) {
        return SparkFactionThirdFactionComponent.of(server).settings().dividend();
    }

    /**
     * Sets {@link #thirdFactionDividend}; applies from the next round. Call it on the server thread.
     * 设置 {@link #thirdFactionDividend}；从下一局生效。请在服务端线程调用。
     *
     * @throws IllegalArgumentException {@code dividend < 3}
     * @since 2.0.0
     */
    public static void setThirdFactionDividend(MinecraftServer server, int dividend) {
        SparkFactionThirdFactionComponent.of(server).setDividend(dividend);
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
