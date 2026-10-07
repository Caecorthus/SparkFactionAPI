package dev.caecorthus.sparkfactionapi.api.cooldown;

import dev.caecorthus.sparkfactionapi.impl.cooldown.ForcedCooldownRegistry;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Unified, monotonic forced-cooldown contract for role skills and carried items. Features that "force a cooldown"
 * on another player (penalties, auras, debuffs) go through this class instead of writing each mod's counters.
 * Every write is monotonic (never shortens), runs on the server thread, and is exact: item cooldowns written here
 * bypass cooldown modifiers such as SparkTraits Fast Hands, so a penalty keeps its stated length. Registration
 * happens during mod initialization; registration order is observable (it is the {@link #slots} order).
 * Affect vetoes are the caller's job: call {@code SparkFactionApi.canAffectPlayer} first. The one shortening entry
 * point, {@link #clearItemKeepingForced}, clears an item's natural cooldown and keeps what was forced.
 * 职业技能与携带物品的统一、单调强制冷却契约。对他人"强制冷却"的功能（惩罚、光环、减益）都走这里，而不是分别写各模组的
 * 计数。每次写入都单调（绝不缩短）、在服务端线程执行且精确：经此写入的物品冷却会越过 SparkTraits 快手等冷却倍率，
 * 惩罚保持其标称时长。注册在模组初始化时进行，注册顺序可观察（即 {@link #slots} 的顺序）。影响否决由调用方负责：
 * 请先调用 {@code SparkFactionApi.canAffectPlayer}。唯一会缩短冷却的入口 {@link #clearItemKeepingForced} 清除物品的
 * 自然冷却并保留强制部分。
 */
public final class ForcedCooldowns {
    private ForcedCooldowns() {
    }

    /**
     * Registers a role-skill counter. Ids must be unique; a duplicate id throws {@link IllegalArgumentException}.
     * 注册职业技能计数；id 必须唯一，重复时抛出 {@link IllegalArgumentException}。
     */
    public static void registerRoleSkillStore(RoleSkillCooldownStore store) {
        ForcedCooldownRegistry.registerRoleSkillStore(store);
    }

    /**
     * Registers a nominal-cooldown provider for items. Lookup order: explicit {@link #registerItemNominal} values,
     * then providers in registration order, then Wathe's {@code GameConstants.ITEM_COOLDOWNS}.
     * 注册物品标准冷却提供者。查找顺序：显式登记值、按注册顺序的提供者、Wathe 的 ITEM_COOLDOWNS。
     */
    public static void registerItemNominalProvider(ItemCooldownNominalProvider provider) {
        ForcedCooldownRegistry.registerItemNominalProvider(provider);
    }

    /** Registers one item's nominal cooldown; the last registration for an item wins. / 登记单个物品的标准冷却，后者覆盖前者。 */
    public static void registerItemNominal(Item item, int ticks) {
        ForcedCooldownRegistry.registerItemNominal(item, ticks);
    }

    /**
     * Registers an item exemption: exempt items never appear as slots and are never written. NoellesRoles'
     * {@code noellesroles:timed_bomb} is exempt by default (its cooldown is the Bomber pass gate).
     * 注册物品豁免：被豁免的物品不出现在 slots 中且永不写入。NoellesRoles 定时炸弹默认豁免（其冷却是炸弹客的转手门槛）。
     */
    public static void registerItemExemption(Predicate<Item> exemption) {
        ForcedCooldownRegistry.registerItemExemption(exemption);
    }

    /**
     * Every forcible cooldown of the player: role-skill stores (registration order) whose {@code appliesTo} and
     * {@code mayForce} are true, then each distinct non-exempt carried item (main inventory, offhand, armor order),
     * including items that are ready now.
     * 玩家的所有可强制冷却：先是 appliesTo 与 mayForce 均为真的职业技能存储（按注册顺序），再是每种未豁免的携带物品
     * （主背包、副手、盔甲顺序），包括当前已就绪的物品。
     */
    public static List<CooldownSlot> slots(ServerPlayerEntity player) {
        return ForcedCooldownRegistry.slots(player);
    }

    /** {@link #slots(ServerPlayerEntity)} filtered to one kind. / 按类别过滤的 slots。 */
    public static List<CooldownSlot> slots(ServerPlayerEntity player, CooldownKind kind) {
        return ForcedCooldownRegistry.slots(player, kind);
    }

    /**
     * Floors one slot: afterwards at least {@code ticks} remain; never shortens. Returns true when it wrote.
     * 为单项设下限：之后至少剩余 {@code ticks}；绝不缩短。写入时返回 true。
     */
    public static boolean raise(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        return ForcedCooldownRegistry.raise(player, slot, ticks);
    }

    /**
     * Extends one slot by {@code ticks} (a ready slot starts a fresh cooldown). Returns true when it wrote.
     * 将单项延长 {@code ticks}（已就绪的从头开始冷却）。写入时返回 true。
     */
    public static boolean extend(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        return ForcedCooldownRegistry.extend(player, slot, ticks);
    }

    /** Floors every slot of the player; returns how many were written. / 为玩家所有项设下限，返回写入数量。 */
    public static int raiseAll(ServerPlayerEntity player, int ticks) {
        return ForcedCooldownRegistry.raiseAll(player, null, ticks);
    }

    /** Floors every slot of one kind; returns how many were written. / 为某一类别的所有项设下限，返回写入数量。 */
    public static int raiseAll(ServerPlayerEntity player, CooldownKind kind, int ticks) {
        return ForcedCooldownRegistry.raiseAll(player, kind, ticks);
    }

    /** Nominal cooldown of an item per the lookup order above. / 按上述顺序查得的物品标准冷却。 */
    public static OptionalInt itemNominalTicks(Item item) {
        return ForcedCooldownRegistry.itemNominalTicks(item);
    }

    /** True when the item is exempt from forced cooldowns. / 物品是否豁免强制冷却。 */
    public static boolean isItemExempt(Item item) {
        return ForcedCooldownRegistry.isItemExempt(item);
    }

    /** Remaining ticks of a vanilla item cooldown on this player. / 该玩家某物品原版冷却的剩余 tick。 */
    public static int itemRemainingTicks(ServerPlayerEntity player, Item item) {
        return ForcedCooldownRegistry.itemRemainingTicks(player, item);
    }

    /**
     * Clears one item's cooldown the way a role mechanic (a refresh, a reset on kill) should: like
     * {@code ItemCooldownManager.remove}, but a forced cooldown still pending on the item survives. Natural, and
     * cleared: the item's own use cooldown and every owner timer that {@code remove} hooks release (for example
     * SparkWitch's Clock and Ceremonial Sword timers). Kept, re-installed exactly: what {@link #raise}, {@link #extend}
     * or {@link #raiseAll} forced onto this item and is not yet served (a raise runs from its write; an extension waits
     * behind the time it was appended to), never more than the item had left. A forced part already dropped by a later
     * plain {@code remove} (admin {@code clearCooldown}, Wathe reset) or replaced by a {@code set} is not restored.
     * Role-skill stores are never read or written; no affect veto is consulted. Returns the forced ticks kept, 0 when
     * the item is now ready. Server thread only.
     * 以职业机制（刷新、击杀重置）应有的方式清除单个物品的冷却：效果同 {@code ItemCooldownManager.remove}，但物品上尚未
     * 结束的强制冷却会保留。自然部分（被清除）：物品自身的使用冷却，以及 {@code remove} 钩子释放的所属模组计时（例如
     * SparkWitch 怀表与仪礼剑计时）。强制部分（保留并精确重装）：经 raise、extend 或 raiseAll 施加到该物品且尚未消耗的
     * 部分（raise 自写入起计时；extend 须等其前面已有的冷却走完才开始消耗），绝不超过物品原本的剩余时间。之后已被
     * 普通 remove（管理员 clearCooldown、Wathe 重置）丢弃或被 set 替换的强制部分不会恢复。职业技能存储既不读取也不
     * 写入；不做影响否决。返回保留的强制 tick，物品已就绪时为 0。仅限服务端线程。
     */
    public static int clearItemKeepingForced(ServerPlayerEntity player, Item item) {
        return ForcedCooldownRegistry.clearItemKeepingForced(player, item);
    }
}
