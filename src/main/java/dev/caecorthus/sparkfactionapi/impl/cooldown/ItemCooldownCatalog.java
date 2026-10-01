package dev.caecorthus.sparkfactionapi.impl.cooldown;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Item nominal-cooldown lookup and exemptions, generic over the item type so the ordering is testable without a
 * Minecraft bootstrap. Lookup order: explicit values (last wins), then providers in registration order (first
 * present wins), then the host fallback (Wathe {@code GameConstants.ITEM_COOLDOWNS}). Negative values are "unknown".
 * 物品标准冷却查找与豁免；对物品类型泛型化，使顺序无需 Minecraft 引导即可测试。查找顺序：显式值（后者覆盖）、
 * 按注册顺序的提供者（首个有值者胜出）、宿主兜底（Wathe 的 ITEM_COOLDOWNS）。负值视为未知。
 */
final class ItemCooldownCatalog<I> {
    /**
     * Built-in exemptions matched by registry id, so SparkFactionAPI needs no NoellesRoles class. The timed bomb's
     * cooldown is the Bomber pass gate; forcing it would let or stop a pass outside the Bomber's rules.
     * 按注册 id 匹配的内置豁免，SparkFactionAPI 因此无需引用 NoellesRoles 的类。定时炸弹的冷却是炸弹客的转手门槛，
     * 强制写入会在炸弹客规则之外放行或阻止转手。
     */
    static final Set<Identifier> DEFAULT_EXEMPT_IDS = Set.of(Identifier.of("noellesroles", "timed_bomb"));

    private final Function<? super I, Identifier> idOf;
    private final Function<? super I, Integer> hostNominal;
    private final IsolatedFailures failures;
    private final Map<I, Integer> explicitNominals = new ConcurrentHashMap<>();
    private final List<Function<? super I, OptionalInt>> providers = new CopyOnWriteArrayList<>();
    private final List<Predicate<? super I>> exemptions = new CopyOnWriteArrayList<>();

    ItemCooldownCatalog(
            Function<? super I, Identifier> idOf,
            Function<? super I, Integer> hostNominal,
            IsolatedFailures failures
    ) {
        this.idOf = Objects.requireNonNull(idOf, "idOf");
        this.hostNominal = Objects.requireNonNull(hostNominal, "hostNominal");
        this.failures = Objects.requireNonNull(failures, "failures");
    }

    void registerNominal(I item, int ticks) {
        Objects.requireNonNull(item, "item");
        if (ticks < 0) {
            throw new IllegalArgumentException("Nominal cooldown must not be negative: " + ticks);
        }
        explicitNominals.put(item, ticks);
    }

    void registerProvider(Function<? super I, OptionalInt> provider) {
        providers.add(Objects.requireNonNull(provider, "provider"));
    }

    void registerExemption(Predicate<? super I> exemption) {
        exemptions.add(Objects.requireNonNull(exemption, "exemption"));
    }

    OptionalInt nominalTicks(I item) {
        if (item == null) {
            return OptionalInt.empty();
        }
        Integer explicit = explicitNominals.get(item);
        if (explicit != null) {
            return OptionalInt.of(explicit);
        }
        for (int index = 0; index < providers.size(); index++) {
            try {
                OptionalInt provided = providers.get(index).apply(item);
                if (provided != null && provided.isPresent() && provided.getAsInt() >= 0) {
                    return provided;
                }
            } catch (RuntimeException | LinkageError failure) {
                failures.report("Item nominal-cooldown provider #" + index, "treated as no answer", failure);
            }
        }
        Integer host = hostNominal.apply(item);
        return host != null && host >= 0 ? OptionalInt.of(host) : OptionalInt.empty();
    }

    /**
     * True for built-in ids and registered predicates. A predicate that throws fails closed (exempt), so an item
     * that cannot be classified is never written.
     * 内置 id 与已注册谓词均可豁免。抛异常的谓词按失败关闭处理（视为豁免），无法判定的物品永不写入。
     */
    boolean isExempt(I item) {
        if (item == null) {
            return true;
        }
        if (DEFAULT_EXEMPT_IDS.contains(idOf.apply(item))) {
            return true;
        }
        for (int index = 0; index < exemptions.size(); index++) {
            try {
                if (exemptions.get(index).test(item)) {
                    return true;
                }
            } catch (RuntimeException | LinkageError failure) {
                failures.report("Item cooldown exemption #" + index, "item treated as exempt", failure);
                return true;
            }
        }
        return false;
    }
}
