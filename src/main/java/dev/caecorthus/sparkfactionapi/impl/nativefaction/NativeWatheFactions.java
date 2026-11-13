package dev.caecorthus.sparkfactionapi.impl.nativefaction;

import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.doctor4t.wathe.api.Faction;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the Wathe {@code Faction} constants that SparkFactionAPI appends from fabric.mod.json declarations, and the
 * two-way mapping between every Wathe {@code Faction} and its faction id.
 * <p>
 * Initialization contract: {@link #install} is called exactly once, from {@code FactionEnumExtensionMixin} at the TAIL
 * of {@code Faction.<clinit>}. This class's own static initializer must never touch {@code Faction} (that could run
 * {@code install} before this class finished initializing); readers call {@link #ensureFactionInitialized} first, so a
 * lookup can never observe the table before Wathe's {@code Faction} class exists.
 * 持有 SparkFactionAPI 依据 fabric.mod.json 声明追加的 Wathe {@code Faction} 常量，以及每个 Wathe {@code Faction}
 * 与阵营 id 的双向映射。初始化契约：{@link #install} 只会在 {@code Faction.<clinit>} 的 TAIL 由
 * {@code FactionEnumExtensionMixin} 调用一次。本类自身的静态初始化绝不能触碰 {@code Faction}（否则可能在本类
 * 初始化完成前就执行 {@code install}）；读取方先调用 {@link #ensureFactionInitialized}，因此查询不会早于
 * Wathe {@code Faction} 类存在。
 */
public final class NativeWatheFactions {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/NativeWatheFactions");
    // Null until install; an empty table means "no native faction declared". 安装前为 null；空表表示没有声明原生阵营。
    private static volatile @Nullable Table table;

    private NativeWatheFactions() {
    }

    /**
     * Reads every mod's declaration and returns the validated, sorted plan to append after {@code original}.
     * 读取所有模组的声明，返回要追加在 {@code original} 之后、已校验且排好序的计划。
     */
    public static List<NativeWatheFactionDeclarations.Declaration> planFromFabricMetadata(Faction[] original) {
        List<String> names = new ArrayList<>(original.length);
        for (Faction faction : original) {
            names.add(faction.name());
        }
        try {
            return NativeWatheFactionDeclarations.plan(NativeWatheFactionDeclarations.readFabricMetadata(), names);
        } catch (RuntimeException exception) {
            LOGGER.error("SparkFactionAPI cannot extend dev.doctor4t.wathe.api.Faction; fix the mod's fabric.mod.json. "
                    + "SparkFactionAPI 无法扩展 Wathe Faction，请修正该模组的 fabric.mod.json。", exception);
            throw exception;
        }
    }

    /**
     * Records the appended constants and returns the new {@code Faction.$VALUES}: {@code original} followed by
     * {@code created}. {@code created[i]} must be named {@code plan.get(i).constantName()} with ordinal
     * {@code original.length + i}. Logs one INFO line listing the appended constants.
     * 记录追加的常量并返回新的 {@code Faction.$VALUES}：{@code original} 后接 {@code created}。
     * {@code created[i]} 的名字必须是 {@code plan.get(i).constantName()}，序号为 {@code original.length + i}。
     * 会输出一行 INFO 日志列出追加的常量。
     *
     * @throws IllegalStateException when called twice or when a created constant does not match the plan
     *                               重复调用或常量与计划不符时
     */
    public static synchronized Faction[] install(
            Faction[] original,
            List<NativeWatheFactionDeclarations.Declaration> plan,
            Faction[] created
    ) {
        if (created.length != plan.size()) {
            throw new IllegalStateException("Expected " + plan.size() + " native Wathe factions, got " + created.length);
        }
        Faction[] extended = new Faction[original.length + created.length];
        System.arraycopy(original, 0, extended, 0, original.length);
        Identifier[] ids = new Identifier[created.length];
        Map<Identifier, Faction> byId = new HashMap<>();
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < created.length; i++) {
            NativeWatheFactionDeclarations.Declaration declaration = plan.get(i);
            Faction constant = created[i];
            int ordinal = original.length + i;
            if (constant == null || !constant.name().equals(declaration.constantName()) || constant.ordinal() != ordinal) {
                throw new IllegalStateException("Native Wathe faction " + declaration.constantName() + " must have ordinal "
                        + ordinal + ", got " + constant);
            }
            extended[ordinal] = constant;
            ids[i] = declaration.factionId();
            byId.put(declaration.factionId(), constant);
            summary.append(i == 0 ? "" : ", ")
                    .append(constant.name()).append('=').append(declaration.factionId())
                    .append(" (ordinal ").append(ordinal).append(", ").append(String.join("+", declaration.modIds()))
                    .append(')');
        }
        // Checked after the plan so a mismatch is reported as such. 在校验计划之后检查，使不匹配错误如实报告。
        if (table != null) {
            throw new IllegalStateException("Native Wathe factions were already installed");
        }
        table = new Table(original.length, created.clone(), ids, Collections.unmodifiableMap(byId));
        if (created.length == 0) {
            LOGGER.info("SparkFactionAPI native Wathe factions: none declared; Faction keeps its {} constants. "
                    + "未声明原生 Wathe 阵营。", original.length);
        } else {
            LOGGER.info("SparkFactionAPI appended native Wathe factions to dev.doctor4t.wathe.api.Faction: {}. "
                    + "已追加原生 Wathe 阵营。", summary);
        }
        return extended;
    }

    /**
     * Forces Wathe's {@code Faction} class (and so {@link #install}) to have run. Never call it from a static
     * initializer of this package.
     * 强制 Wathe {@code Faction} 类（因而 {@link #install}）已经执行；不要在本包的静态初始化中调用。
     */
    public static void ensureFactionInitialized() {
        // A getstatic triggers Faction.<clinit>; afterwards it is a plain field read. getstatic 会触发初始化，之后只是普通读取。
        Faction ignored = Faction.NONE;
    }

    /** Declared native custom id → its appended constant, otherwise null (built-ins return null). */
    public static @Nullable Faction customConstantOrNull(@Nullable Identifier factionId) {
        if (factionId == null) {
            return null;
        }
        ensureFactionInitialized();
        Table current = table;
        return current == null ? null : current.byId().get(factionId);
    }

    /**
     * Reverse lookup without a switch: built-in constants map to {@code wathe:*}, appended ones to their declared id,
     * and anything else (for example a constant appended by another mod) to null. Allocation-free.
     * 不使用 switch 的反查：内置常量映射到 {@code wathe:*}，追加的常量映射到声明的 id，其它（例如其它模组追加的常量）
     * 返回 null。不分配对象。
     */
    public static @Nullable Identifier factionIdOrNull(@Nullable Faction faction) {
        if (faction == null) {
            return null;
        }
        if (faction == Faction.NONE) {
            return FactionIds.NONE;
        }
        if (faction == Faction.CIVILIAN) {
            return FactionIds.CIVILIAN;
        }
        if (faction == Faction.KILLER) {
            return FactionIds.KILLER;
        }
        if (faction == Faction.NEUTRAL) {
            return FactionIds.NEUTRAL;
        }
        return customFactionIdOrNull(faction);
    }

    /** Declared id of an appended constant, otherwise null (built-ins return null). Allocation-free. */
    public static @Nullable Identifier customFactionIdOrNull(@Nullable Faction faction) {
        Table current = table;
        if (faction == null || current == null) {
            return null;
        }
        int index = faction.ordinal() - current.base();
        return index >= 0 && index < current.constants().length && current.constants()[index] == faction
                ? current.ids()[index]
                : null;
    }

    public static Optional<Identifier> factionId(@Nullable Faction faction) {
        return Optional.ofNullable(factionIdOrNull(faction));
    }

    public static boolean isNativeCustomFaction(@Nullable Identifier factionId) {
        return customConstantOrNull(factionId) != null;
    }

    private record Table(int base, Faction[] constants, Identifier[] ids, Map<Identifier, Faction> byId) {
    }
}
