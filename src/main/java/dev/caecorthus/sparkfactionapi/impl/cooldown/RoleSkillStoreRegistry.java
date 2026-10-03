package dev.caecorthus.sparkfactionapi.impl.cooldown;

import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownKind;
import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownSlot;
import dev.caecorthus.sparkfactionapi.api.cooldown.RoleSkillCooldownStore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registration-ordered role-skill stores. Every downstream callback is isolated: a store that throws is logged,
 * treated as "not listed" / "not written", and never stops the remaining stores. Thread checks live in
 * {@link ForcedCooldownRegistry}; this class only orders, filters and delegates.
 * 按注册顺序保存的职业技能存储。每个下游回调都被隔离：抛异常的存储会被记录，并视为"不列出"/"未写入"，不会阻断其余存储。
 * 线程检查在 {@link ForcedCooldownRegistry} 中；本类只负责排序、过滤与委托。
 */
final class RoleSkillStoreRegistry {
    private final List<Registered> stores = new CopyOnWriteArrayList<>();
    private final IsolatedFailures failures;

    RoleSkillStoreRegistry(IsolatedFailures failures) {
        this.failures = Objects.requireNonNull(failures, "failures");
    }

    /** The id is read once here; duplicates throw {@link IllegalArgumentException}. / id 在此读取一次，重复时抛出 IAE。 */
    synchronized void register(RoleSkillCooldownStore store) {
        Objects.requireNonNull(store, "store");
        Identifier id = Objects.requireNonNull(store.id(), "store.id()");
        for (Registered registered : stores) {
            if (registered.id().equals(id)) {
                throw new IllegalArgumentException("Duplicate role-skill cooldown store id: " + id);
            }
        }
        stores.add(new Registered(id, store));
    }

    List<CooldownSlot> slots(ServerPlayerEntity player) {
        List<CooldownSlot> slots = new ArrayList<>();
        for (Registered registered : stores) {
            try {
                if (forcible(registered.store(), player)) {
                    RoleSkillCooldownStore store = registered.store();
                    slots.add(new CooldownSlot(
                            CooldownKind.ROLE_SKILL,
                            registered.id(),
                            store.remainingTicks(player),
                            store.nominalTicks(player)
                    ));
                }
            } catch (RuntimeException | LinkageError failure) {
                failures.report(source(registered), "slot not listed", failure);
            }
        }
        return slots;
    }

    /** Re-checks appliesTo/mayForce, then delegates to the store's own {@code raiseTo}. / 重新检查后委托 raiseTo。 */
    boolean raise(ServerPlayerEntity player, Identifier id, int ticks) {
        Registered registered = find(id);
        if (registered == null) {
            return false;
        }
        try {
            return forcible(registered.store(), player) && registered.store().raiseTo(player, ticks);
        } catch (RuntimeException | LinkageError failure) {
            failures.report(source(registered), "raise not written", failure);
            return false;
        }
    }

    /** Re-checks appliesTo/mayForce, then delegates to the store's own {@code extendBy}. / 重新检查后委托 extendBy。 */
    boolean extend(ServerPlayerEntity player, Identifier id, int ticks) {
        Registered registered = find(id);
        if (registered == null) {
            return false;
        }
        try {
            return forcible(registered.store(), player) && registered.store().extendBy(player, ticks);
        } catch (RuntimeException | LinkageError failure) {
            failures.report(source(registered), "extend not written", failure);
            return false;
        }
    }

    private Registered find(Identifier id) {
        for (Registered registered : stores) {
            if (registered.id().equals(id)) {
                return registered;
            }
        }
        return null;
    }

    // mayForce is only asked for stores that apply to the player. / 只有适用于该玩家的存储才会被询问 mayForce。
    private static boolean forcible(RoleSkillCooldownStore store, ServerPlayerEntity player) {
        return store.appliesTo(player) && store.mayForce(player);
    }

    private static String source(Registered registered) {
        return "Role-skill cooldown store " + registered.id();
    }

    private record Registered(Identifier id, RoleSkillCooldownStore store) {
    }
}
