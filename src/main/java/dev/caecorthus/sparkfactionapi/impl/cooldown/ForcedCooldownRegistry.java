package dev.caecorthus.sparkfactionapi.impl.cooldown;

import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownKind;
import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownSlot;
import dev.caecorthus.sparkfactionapi.api.cooldown.ItemCooldownNominalProvider;
import dev.caecorthus.sparkfactionapi.api.cooldown.RoleSkillCooldownStore;
import dev.caecorthus.sparkfactionapi.mixin.cooldown.ItemCooldownEntryAccessor;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Runtime behind {@code api/cooldown/ForcedCooldowns}. Registration may happen from any thread during mod
 * initialization. Every method that takes a player is server-side only and throws {@link IllegalStateException}
 * when the player's server is missing or the caller is not on that server's thread; a {@code null} player, slot or
 * kind throws {@link NullPointerException}. Slots list role-skill stores (registration order) before carried items
 * (main inventory, offhand, armor). Writes are monotonic and re-validate the live state: a ROLE_SKILL write
 * re-checks {@code appliesTo && mayForce}; an ITEM write re-checks the exemption and that the item is still carried,
 * and records its forced part on the live vanilla entry ({@link ForcedItemLedger}) for {@code clearItemKeepingForced}.
 * {@code ForcedCooldowns} 的运行时实现。注册可在模组初始化期间于任意线程进行。所有接收玩家的方法仅限服务端：
 * 玩家无服务器或调用方不在该服务器线程时抛出 {@link IllegalStateException}；玩家、slot 或类别为 null 时抛出 NPE。
 * slots 先列职业技能存储（注册顺序），再列携带物品（主背包、副手、盔甲）。写入单调且会重新校验实时状态：职业技能写入
 * 重新检查 appliesTo 与 mayForce；物品写入重新检查豁免以及玩家是否仍携带该物品，并在实时原版条目上记录强制部分
 * （{@link ForcedItemLedger}），供 {@code clearItemKeepingForced} 使用。
 */
public final class ForcedCooldownRegistry {
    private static final IsolatedFailures FAILURES = new IsolatedFailures();
    private static final RoleSkillStoreRegistry ROLE_SKILLS = new RoleSkillStoreRegistry(FAILURES);
    // Lambdas keep Registries and Wathe GameConstants lazy until the first lookup.
    // 使用 lambda，使 Registries 与 Wathe GameConstants 直到首次查找时才加载。
    private static final ItemCooldownCatalog<Item> ITEMS = new ItemCooldownCatalog<>(
            item -> Registries.ITEM.getId(item),
            item -> GameConstants.ITEM_COOLDOWNS.get(item),
            FAILURES
    );
    private static final ForcedItemLedger FORCED_ITEMS = new ForcedItemLedger();

    private ForcedCooldownRegistry() {
    }

    public static void registerRoleSkillStore(RoleSkillCooldownStore store) {
        ROLE_SKILLS.register(store);
    }

    public static void registerItemNominalProvider(ItemCooldownNominalProvider provider) {
        Objects.requireNonNull(provider, "provider");
        ITEMS.registerProvider(provider::nominalTicks);
    }

    public static void registerItemNominal(Item item, int ticks) {
        ITEMS.registerNominal(item, ticks);
    }

    public static void registerItemExemption(Predicate<Item> exemption) {
        ITEMS.registerExemption(exemption);
    }

    public static List<CooldownSlot> slots(ServerPlayerEntity player) {
        return Collections.unmodifiableList(collect(requireServerThread(player), null));
    }

    public static List<CooldownSlot> slots(ServerPlayerEntity player, CooldownKind kind) {
        Objects.requireNonNull(kind, "kind");
        return Collections.unmodifiableList(collect(requireServerThread(player), kind));
    }

    public static boolean raise(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        return write(requireServerThread(player), Objects.requireNonNull(slot, "slot"), ticks, false);
    }

    public static boolean extend(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        return write(requireServerThread(player), Objects.requireNonNull(slot, "slot"), ticks, true);
    }

    /** {@code kind == null} means every kind. / kind 为 null 表示全部类别。 */
    public static int raiseAll(ServerPlayerEntity player, CooldownKind kind, int ticks) {
        requireServerThread(player);
        int written = 0;
        for (CooldownSlot slot : collect(player, kind)) {
            if (write(player, slot, ticks, false)) {
                written++;
            }
        }
        return written;
    }

    public static OptionalInt itemNominalTicks(Item item) {
        return ITEMS.nominalTicks(item);
    }

    public static boolean isItemExempt(Item item) {
        return ITEMS.isExempt(item);
    }

    public static int itemRemainingTicks(ServerPlayerEntity player, Item item) {
        requireServerThread(player);
        return item == null ? 0 : ExactItemCooldownWriter.remainingTicks(player.getItemCooldownManager(), item);
    }

    private static List<CooldownSlot> collect(ServerPlayerEntity player, CooldownKind kind) {
        List<CooldownSlot> slots = new ArrayList<>();
        if (kind == null || kind == CooldownKind.ROLE_SKILL) {
            slots.addAll(ROLE_SKILLS.slots(player));
        }
        if (kind == null || kind == CooldownKind.ITEM) {
            for (Item item : carriedItems(player)) {
                slots.add(new CooldownSlot(
                        CooldownKind.ITEM,
                        Registries.ITEM.getId(item),
                        ExactItemCooldownWriter.remainingTicks(player.getItemCooldownManager(), item),
                        ITEMS.nominalTicks(item)
                ));
            }
        }
        return slots;
    }

    private static boolean write(ServerPlayerEntity player, CooldownSlot slot, int ticks, boolean extend) {
        if (slot.kind() == CooldownKind.ROLE_SKILL) {
            return extend ? ROLE_SKILLS.extend(player, slot.id(), ticks) : ROLE_SKILLS.raise(player, slot.id(), ticks);
        }
        Optional<Item> resolved = Registries.ITEM.getOrEmpty(slot.id());
        if (resolved.isEmpty()) {
            return false;
        }
        Item item = resolved.get();
        // The slot is only a snapshot: exemptions and possession are re-checked against the live player.
        // slot 只是快照：豁免与持有状态需对照实时玩家重新检查。
        if (ITEMS.isExempt(item) || !carries(player, item)) {
            return false;
        }
        ItemCooldownManager manager = player.getItemCooldownManager();
        int current = ExactItemCooldownWriter.remainingTicks(manager, item);
        OptionalInt target = extend
                ? ForcedCooldownRules.extendTarget(current, ticks)
                : ForcedCooldownRules.raiseTarget(current, ticks);
        if (target.isPresent() && !ExactItemCooldownWriter.writeExact(player, item, target.getAsInt())) {
            return false;
        }
        // Record the forced part on the live entry for clearItemKeepingForced: a raise is a floor from now (recorded
        // even when a longer natural cooldown covers it), an extend is appended after the `current` ticks.
        // 在实时条目上记录强制部分，供 clearItemKeepingForced 使用：raise 是自此刻起的下限（即使更长的自然冷却已覆盖
        // 也会记录），extend 追加在原有 `current` tick 之后。
        ItemCooldownEntryAccessor entry = ExactItemCooldownWriter.liveEntry(manager, item);
        if (entry != null) {
            if (extend) {
                FORCED_ITEMS.extended(entry, current, ticks);
            } else {
                FORCED_ITEMS.raised(entry, ExactItemCooldownWriter.tick(manager), ticks);
            }
        }
        return target.isPresent();
    }

    /**
     * Plain {@code remove} (owner remove hooks run and release their own timers), then the forced part that was still
     * covering the item is installed again exactly, never shortening a cooldown a hook just set.
     * 先普通 {@code remove}（所属模组的 remove 钩子照常运行并释放自有计时），再精确重装仍覆盖该物品的强制部分；
     * 绝不缩短钩子刚设置的冷却。
     */
    public static int clearItemKeepingForced(ServerPlayerEntity player, Item item) {
        requireServerThread(player);
        Objects.requireNonNull(item, "item");
        ItemCooldownManager manager = player.getItemCooldownManager();
        ItemCooldownEntryAccessor before = ExactItemCooldownWriter.liveEntry(manager, item);
        int kept = before == null ? 0 : FORCED_ITEMS.keptTicks(
                before, before.sparkfactionapi$getEndTick(), ExactItemCooldownWriter.tick(manager));
        manager.remove(item);
        if (kept <= 0) {
            return 0;
        }
        int current = ExactItemCooldownWriter.remainingTicks(manager, item);
        if (ForcedCooldownRules.raiseTarget(current, kept).isPresent()
                && !ExactItemCooldownWriter.writeExact(player, item, kept)) {
            return 0;
        }
        ItemCooldownEntryAccessor after = ExactItemCooldownWriter.liveEntry(manager, item);
        if (after == null) {
            return 0;
        }
        FORCED_ITEMS.raised(after, ExactItemCooldownWriter.tick(manager), kept);
        return kept;
    }

    private static List<Item> carriedItems(ServerPlayerEntity player) {
        return ForcedCooldownRules.distinctCarriedItems(sections(player.getInventory()), ItemStack::isEmpty,
                ItemStack::getItem, ITEMS::isExempt);
    }

    private static boolean carries(ServerPlayerEntity player, Item item) {
        return ForcedCooldownRules.carries(sections(player.getInventory()), ItemStack::isEmpty, ItemStack::getItem, item);
    }

    // Public slot order: main inventory, then offhand, then armor. / 公开的槽位顺序：主背包、副手、盔甲。
    private static List<DefaultedList<ItemStack>> sections(PlayerInventory inventory) {
        return List.of(inventory.main, inventory.offHand, inventory.armor);
    }

    private static ServerPlayerEntity requireServerThread(ServerPlayerEntity player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.getServer();
        if (server == null || !server.isOnThread()) {
            throw new IllegalStateException("ForcedCooldowns must be called on the server thread");
        }
        return player;
    }
}
