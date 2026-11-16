package dev.caecorthus.sparkfactionapi.impl.thirdfaction;

import dev.caecorthus.sparkfactionapi.api.FactionCategory;
import dev.caecorthus.sparkfactionapi.api.ThirdFactionDrawContext;
import dev.caecorthus.sparkfactionapi.api.ThirdFactionLineup;
import dev.caecorthus.sparkfactionapi.component.SparkFactionThirdFactionComponent;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactions;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter of the third-faction draw: gathers the round's state, asks {@link ThirdFactionDrawRules#plan} and deals the
 * plan with {@code GameWorldComponent#addRole}. Called from {@code MurderGameModeMixin} after the AFTER_VIGILANTES
 * assignment phase, i.e. right before Wathe's {@code assignNeutrals}; Wathe's forced roles and killer/vigilante passes
 * already ran, so those roles count as present.
 * 第三方阵营抽取的适配器：收集本局状态，调用 {@link ThirdFactionDrawRules#plan}，并用 {@code GameWorldComponent#addRole}
 * 执行计划。由 {@code MurderGameModeMixin} 在 AFTER_VIGILANTES 分配阶段之后调用，即紧接在 Wathe {@code assignNeutrals}
 * 之前；此时 Wathe 的强制角色与杀手/义警分配已完成，这些角色计为在场。
 */
public final class ThirdFactionDraw {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/ThirdFactionDraw");

    // Third factions present once the draw is done, read by Wathe's neutral pass of the same assignment; null outside
    // it. Server thread only. 抽取完成后在场的第三方阵营，供同一次分配中的 Wathe 中立分配读取；其它时刻为 null。仅服务端线程。
    private static volatile @Nullable Set<Faction> neutralPassClaims;

    private ThirdFactionDraw() {
    }

    /**
     * Category of a role's real Wathe faction ({@code role.getFaction()} outside Wathe's neutral pass, so a bound or
     * registered native role reads its appended constant). Null is NONE.
     * 角色真实 Wathe 阵营的分类（在 Wathe 中立分配之外读取 {@code role.getFaction()}，因此绑定或注册的原生角色读到其追加
     * 常量）。null 为 NONE。
     */
    public static FactionCategory categoryOf(@Nullable Role role) {
        return role == null ? FactionCategory.NONE : ThirdFactionDrawRules.categoryOf(role.getFaction());
    }

    public static ThirdFactionDrawSettings settings(ServerWorld world) {
        return SparkFactionThirdFactionComponent.of(world.getServer()).settings();
    }

    /**
     * Runs the draw. Fails closed: an unexpected failure is logged, deals nothing more and leaves Wathe's neutral pool
     * as it is without a draw, so the round still starts.
     * 执行抽取。失败即关闭：意外失败会记录日志、不再发放，并让 Wathe 中立池保持没有抽取时的样子，对局仍能开始。
     */
    public static void drawBeforeNeutrals(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players
    ) {
        neutralPassClaims = null;
        try {
            draw(world, gameComponent, players);
        } catch (RuntimeException | LinkageError exception) {
            neutralPassClaims = null;
            LOGGER.error("Third-faction draw failed; no third faction is dealt this round. "
                    + "第三方阵营抽取失败，本局不发放第三方阵营。", exception);
        }
    }

    private static void draw(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players
    ) {
        ThirdFactionDrawSettings settings = settings(world);
        int totalPlayers = players.size();
        int memberCap = ThirdFactionDrawRules.memberCap(totalPlayers, settings.dividend(), settings.minPlayers());
        Map<Faction, List<Role>> present = presentMembers(gameComponent, players);
        List<ServerPlayerEntity> seats = unassignedPlayers(gameComponent, players);
        List<ServerPlayerEntity> playerView = List.copyOf(players);
        Random random = world.getRandom();

        ThirdFactionDrawRules.Plan<Faction, Role> plan = ThirdFactionDrawRules.plan(
                memberCap,
                present,
                lineupFactions(),
                (faction, members) -> askLineup(world, gameComponent, playerView, memberCap, faction, members),
                seats.size(),
                random::nextInt
        );

        List<Role> toDeal = plan.toDeal();
        if (!toDeal.isEmpty()) {
            shuffle(seats, random);
            for (int index = 0; index < toDeal.size(); index++) {
                gameComponent.addRole(seats.get(index), toDeal.get(index));
            }
        }

        Set<Faction> claims = new LinkedHashSet<>(present.keySet());
        if (plan.faction() != null && !toDeal.isEmpty()) {
            claims.add(plan.faction());
        }
        neutralPassClaims = Set.copyOf(claims);
        LOGGER.debug("Third-faction draw: players={}, cap={} (dividend {}, min players {}), outcome={}, faction={}, "
                        + "present={}, dealt={}",
                totalPlayers, memberCap, settings.dividend(), settings.minPlayers(), plan.outcome(),
                plan.faction() == null ? "none" : NativeWatheFactions.factionIdOrNull(plan.faction()),
                present.size(), toDeal.size());
    }

    /**
     * Ends the neutral-pass window opened by {@link #drawBeforeNeutrals}; called before Wathe's civilian pass.
     * 结束 {@link #drawBeforeNeutrals} 打开的中立分配窗口；在 Wathe 平民分配之前调用。
     */
    public static void endNeutralPass() {
        neutralPassClaims = null;
    }

    /**
     * See {@link ThirdFactionDrawRules#neutralPoolOpen}. 见 {@link ThirdFactionDrawRules#neutralPoolOpen}。
     */
    public static boolean neutralPoolOpenFor(Faction faction) {
        return ThirdFactionDrawRules.neutralPoolOpen(neutralPassClaims, faction);
    }

    private static Map<Faction, List<Role>> presentMembers(GameWorldComponent gameComponent, List<ServerPlayerEntity> players) {
        Map<Faction, List<Role>> present = new LinkedHashMap<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role == null || role == WatheRoles.NO_ROLE) {
                continue;
            }
            Faction faction = role.getFaction();
            if (ThirdFactionDrawRules.categoryOf(faction) == FactionCategory.THIRD_FACTION) {
                present.computeIfAbsent(faction, ignored -> new ArrayList<>()).add(role);
            }
        }
        return present;
    }

    private static List<ServerPlayerEntity> unassignedPlayers(GameWorldComponent gameComponent, List<ServerPlayerEntity> players) {
        List<ServerPlayerEntity> unassigned = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role == null || role == WatheRoles.NO_ROLE) {
                unassigned.add(player);
            }
        }
        return unassigned;
    }

    private static List<Faction> lineupFactions() {
        List<Faction> factions = new ArrayList<>();
        for (Identifier factionId : ThirdFactionLineups.factionsWithLineup()) {
            Faction constant = NativeWatheFactions.customConstantOrNull(factionId);
            if (constant != null) {
                factions.add(constant);
            }
        }
        return factions;
    }

    /**
     * Downstream code: a failure is isolated to "this faction deals nothing" so the round still starts.
     * 下游代码：失败只会导致“该阵营不发放”，对局仍能正常开始。
     */
    private static List<Role> askLineup(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            int memberCap,
            Faction faction,
            List<Role> presentMembers
    ) {
        Identifier factionId = NativeWatheFactions.customFactionIdOrNull(faction);
        ThirdFactionLineup lineup = ThirdFactionLineups.lineupOrNull(factionId);
        if (lineup == null) {
            return List.of();
        }
        try {
            List<Role> roles = lineup.rolesToDeal(new ThirdFactionDrawContext(
                    world,
                    gameComponent,
                    players,
                    players.size(),
                    memberCap,
                    factionId,
                    presentMembers
            ));
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            List<Role> dealable = new ArrayList<>(roles.size());
            for (Role role : roles) {
                if (role != null && role != WatheRoles.NO_ROLE) {
                    dealable.add(role);
                }
            }
            return dealable;
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warn("Third-faction lineup of {} failed; it deals nothing this round. "
                    + "第三方阵营 {} 的阵容出错，本局不发放。", factionId, factionId, exception);
            return List.of();
        }
    }

    private static <T> void shuffle(List<T> values, Random random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(swap));
            values.set(swap, value);
        }
    }
}
