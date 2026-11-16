package dev.caecorthus.sparkfactionapi.impl.thirdfaction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * Server-wide third-faction draw settings, persisted by {@code SparkFactionThirdFactionComponent} under the NBT keys
 * {@code Dividend} (default 10, at least 3) and {@code MinPlayers} (default 18, at least 0 = no minimum). A missing
 * key reads as its default; a stored value below the floor is raised to the floor.
 * 全服第三方阵营抽取设置，由 {@code SparkFactionThirdFactionComponent} 以 NBT 键 {@code Dividend}（默认 10，至少 3）与
 * {@code MinPlayers}（默认 18，至少 0 = 无最低人数）持久化。缺少的键读作默认值；低于下限的存储值提升到下限。
 */
public record ThirdFactionDrawSettings(int dividend, int minPlayers) {
    public static final String DIVIDEND_KEY = "Dividend";
    public static final String MIN_PLAYERS_KEY = "MinPlayers";
    public static final ThirdFactionDrawSettings DEFAULTS = new ThirdFactionDrawSettings(
            ThirdFactionDrawRules.DEFAULT_DIVIDEND,
            ThirdFactionDrawRules.DEFAULT_MIN_PLAYERS
    );

    public ThirdFactionDrawSettings {
        requireDividend(dividend);
        requireMinPlayers(minPlayers);
    }

    public ThirdFactionDrawSettings withDividend(int dividend) {
        return new ThirdFactionDrawSettings(dividend, minPlayers);
    }

    public ThirdFactionDrawSettings withMinPlayers(int minPlayers) {
        return new ThirdFactionDrawSettings(dividend, minPlayers);
    }

    public static ThirdFactionDrawSettings fromNbt(NbtCompound tag) {
        int dividend = tag.contains(DIVIDEND_KEY, NbtElement.INT_TYPE)
                ? Math.max(ThirdFactionDrawRules.MIN_DIVIDEND, tag.getInt(DIVIDEND_KEY))
                : ThirdFactionDrawRules.DEFAULT_DIVIDEND;
        int minPlayers = tag.contains(MIN_PLAYERS_KEY, NbtElement.INT_TYPE)
                ? Math.max(ThirdFactionDrawRules.MIN_MIN_PLAYERS, tag.getInt(MIN_PLAYERS_KEY))
                : ThirdFactionDrawRules.DEFAULT_MIN_PLAYERS;
        return new ThirdFactionDrawSettings(dividend, minPlayers);
    }

    public void writeNbt(NbtCompound tag) {
        tag.putInt(DIVIDEND_KEY, dividend);
        tag.putInt(MIN_PLAYERS_KEY, minPlayers);
    }

    private static void requireDividend(int dividend) {
        if (dividend < ThirdFactionDrawRules.MIN_DIVIDEND) {
            throw new IllegalArgumentException("third-faction dividend must be at least "
                    + ThirdFactionDrawRules.MIN_DIVIDEND + ": " + dividend);
        }
    }

    private static void requireMinPlayers(int minPlayers) {
        if (minPlayers < ThirdFactionDrawRules.MIN_MIN_PLAYERS) {
            throw new IllegalArgumentException("third-faction minimum players must be at least "
                    + ThirdFactionDrawRules.MIN_MIN_PLAYERS + ": " + minPlayers);
        }
    }
}
