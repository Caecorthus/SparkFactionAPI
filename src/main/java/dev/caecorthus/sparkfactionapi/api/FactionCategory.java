package dev.caecorthus.sparkfactionapi.api;

/**
 * Coarse category of a Wathe {@code Faction} constant, read with {@link SparkFactionApi#factionCategory}. The four
 * Wathe built-ins map one to one; every other constant (today: the native factions SparkFactionAPI appends from
 * {@code sparkfactionapi:native_wathe_factions}) is {@link #THIRD_FACTION}. A non-native custom faction whose roles
 * sit in a Wathe bucket (for example a NEUTRAL-bucket role) takes that bucket's category, never THIRD_FACTION.
 * Wathe {@code Faction} 常量的粗分类，通过 {@link SparkFactionApi#factionCategory} 读取。四个 Wathe 内置常量一一对应；
 * 其余所有常量（目前即 SparkFactionAPI 依据 {@code sparkfactionapi:native_wathe_factions} 追加的原生阵营）都属于
 * {@link #THIRD_FACTION}。角色位于 Wathe 原生桶中的非原生自定义阵营（例如 NEUTRAL 桶角色）取该桶的分类，绝不是 THIRD_FACTION。
 *
 * @since 2.0.0
 */
public enum FactionCategory {
    NONE("none"),
    CIVILIAN("civilian"),
    NEUTRAL("neutral"),
    KILLER("killer"),
    THIRD_FACTION("third_faction");

    private final String id;

    FactionCategory(String id) {
        this.id = id;
    }

    /**
     * Stable lower-case id ({@code none}, {@code civilian}, {@code neutral}, {@code killer}, {@code third_faction}).
     * 稳定的小写 id。
     */
    public String id() {
        return id;
    }
}
