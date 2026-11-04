package dev.caecorthus.sparkfactionapi.impl.nativefaction;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads and validates the {@code sparkfactionapi:native_wathe_factions} declarations from every mod's
 * {@code fabric.mod.json}. It runs inside Wathe's {@code Faction} static initializer, so it only uses the JDK, Fabric
 * Loader metadata and {@link Identifier} parsing: never {@code Faction.fromRole}, a SparkFactionAPI registry, an
 * {@code EnumMap}/{@code EnumSet} or {@code Enum.valueOf} (those would cache the 4-constant array).
 * 读取并校验所有模组 {@code fabric.mod.json} 中的 {@code sparkfactionapi:native_wathe_factions} 声明。它运行在
 * Wathe {@code Faction} 的静态初始化中，因此只使用 JDK、Fabric Loader 元数据与 {@link Identifier} 解析：
 * 绝不调用 {@code Faction.fromRole}、SparkFactionAPI 注册表、{@code EnumMap}/{@code EnumSet} 或
 * {@code Enum.valueOf}（它们会缓存只有 4 个常量的数组）。
 */
public final class NativeWatheFactionDeclarations {
    /** Custom-value key in fabric.mod.json. fabric.mod.json 中的自定义值键。 */
    public static final String METADATA_KEY = "sparkfactionapi:native_wathe_factions";
    private static final Pattern CONSTANT_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final String RESERVED_NAMESPACE = "wathe";

    private NativeWatheFactionDeclarations() {
    }

    /**
     * One raw {@code "faction id": "CONSTANT"} pair as written by one mod.
     * 某个模组写下的一条原始 {@code "阵营 id": "常量名"}。
     */
    public record Entry(String modId, String factionId, String constantName) {
        public Entry {
            Objects.requireNonNull(modId, "modId");
            Objects.requireNonNull(factionId, "factionId");
            Objects.requireNonNull(constantName, "constantName");
        }
    }

    /**
     * One validated native faction; {@code modIds} lists every mod that declared it (same id, same name).
     * 一个已校验的原生阵营；{@code modIds} 列出所有声明了它（同 id、同名）的模组。
     */
    public record Declaration(Identifier factionId, String constantName, List<String> modIds) {
        public Declaration {
            Objects.requireNonNull(factionId, "factionId");
            Objects.requireNonNull(constantName, "constantName");
            modIds = List.copyOf(modIds);
        }
    }

    /**
     * Collects the raw entries of every loaded mod. A value that is not a JSON object of strings is a hard error.
     * 收集所有已加载模组的原始条目；值不是“字符串到字符串”的 JSON 对象时直接报错。
     */
    public static List<Entry> readFabricMetadata() {
        List<Entry> entries = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            if (!mod.getMetadata().containsCustomValue(METADATA_KEY)) {
                continue;
            }
            CustomValue value = mod.getMetadata().getCustomValue(METADATA_KEY);
            if (value.getType() != CustomValue.CvType.OBJECT) {
                throw invalid(modId, "must be a JSON object of \"namespace:path\": \"CONSTANT_NAME\", got " + value.getType());
            }
            for (Map.Entry<String, CustomValue> pair : value.getAsObject()) {
                CustomValue constant = pair.getValue();
                if (constant.getType() != CustomValue.CvType.STRING) {
                    throw invalid(modId, "value of \"" + pair.getKey() + "\" must be a string constant name, got "
                            + constant.getType());
                }
                entries.add(new Entry(modId, pair.getKey(), constant.getAsString()));
            }
        }
        return entries;
    }

    /**
     * Validates the entries and returns them sorted by the faction id string ("namespace:path"), so client and server
     * append the same constants at the same ordinals whatever order Fabric lists the mods in.
     * Rules: the id has an explicit namespace other than {@code wathe}; the name matches {@code [A-Z][A-Z0-9_]*} and is
     * not an existing constant; one id has one name and one name one id (repeating the same pair is allowed).
     * 校验条目并按阵营 id 字符串（"namespace:path"）排序，使客户端与服务端无论 Fabric 以何种顺序列出模组，
     * 都在相同序号追加相同常量。规则：id 必须显式带命名空间且不是 {@code wathe}；常量名匹配
     * {@code [A-Z][A-Z0-9_]*} 且不是已有常量；一个 id 只能对应一个名字，一个名字只能对应一个 id（重复同一对允许）。
     *
     * @param existingConstantNames names already in {@code Faction.values()} 已存在于 {@code Faction.values()} 的名字
     * @throws IllegalStateException on any invalid or conflicting declaration 任何无效或冲突的声明
     */
    public static List<Declaration> plan(Collection<Entry> entries, Collection<String> existingConstantNames) {
        Set<String> reserved = new HashSet<>(existingConstantNames);
        Map<String, String> nameById = new HashMap<>();
        Map<String, Identifier> parsedById = new HashMap<>();
        Map<String, Set<String>> modsById = new HashMap<>();
        Map<String, String> idByName = new HashMap<>();
        for (Entry entry : entries) {
            Identifier id = parseFactionId(entry);
            String idKey = id.toString();
            String name = entry.constantName();
            if (!CONSTANT_NAME.matcher(name).matches()) {
                throw invalid(entry.modId(), "constant name \"" + name + "\" for " + idKey + " must match [A-Z][A-Z0-9_]*");
            }
            if (reserved.contains(name)) {
                throw invalid(entry.modId(), "constant name " + name + " for " + idKey
                        + " collides with an existing dev.doctor4t.wathe.api.Faction constant");
            }
            String previousName = nameById.get(idKey);
            if (previousName != null && !previousName.equals(name)) {
                throw invalid(entry.modId(), idKey + " is declared as both " + previousName + " (by "
                        + modsById.get(idKey) + ") and " + name);
            }
            String previousId = idByName.get(name);
            if (previousId != null && !previousId.equals(idKey)) {
                throw invalid(entry.modId(), "constant name " + name + " is declared for both " + previousId
                        + " and " + idKey);
            }
            nameById.put(idKey, name);
            parsedById.put(idKey, id);
            idByName.put(name, idKey);
            modsById.computeIfAbsent(idKey, ignored -> new LinkedHashSet<>()).add(entry.modId());
        }

        List<String> idKeys = new ArrayList<>(nameById.keySet());
        idKeys.sort(Comparator.naturalOrder());
        List<Declaration> plan = new ArrayList<>(idKeys.size());
        for (String idKey : idKeys) {
            List<String> mods = new ArrayList<>(modsById.get(idKey));
            mods.sort(Comparator.naturalOrder());
            plan.add(new Declaration(parsedById.get(idKey), nameById.get(idKey), mods));
        }
        return List.copyOf(plan);
    }

    private static Identifier parseFactionId(Entry entry) {
        String raw = entry.factionId();
        if (raw.indexOf(':') <= 0) {
            throw invalid(entry.modId(), "faction id \"" + raw + "\" must be written as namespace:path");
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            throw invalid(entry.modId(), "faction id \"" + raw + "\" is not a valid identifier");
        }
        if (RESERVED_NAMESPACE.equals(id.getNamespace())) {
            throw invalid(entry.modId(), "faction id " + raw + " uses the reserved wathe namespace (built-in factions)");
        }
        return id;
    }

    private static IllegalStateException invalid(String modId, String reason) {
        return new IllegalStateException("Invalid " + METADATA_KEY + " declaration in mod " + modId + ": " + reason
                + " / 模组 " + modId + " 的 " + METADATA_KEY + " 声明无效");
    }
}
