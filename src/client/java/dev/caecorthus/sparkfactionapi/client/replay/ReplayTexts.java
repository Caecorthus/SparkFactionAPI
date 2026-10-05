package dev.caecorthus.sparkfactionapi.client.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Translation keys and text helpers of the replay screen.
 * 回放界面的翻译键与文本工具。
 */
final class ReplayTexts {
    static final String PREFIX = "screen.sparkfactionapi.replay.";
    static final String TITLE = PREFIX + "title";
    static final String SUBTITLE = PREFIX + "subtitle";
    static final String ENDED_AGO = PREFIX + "ended_ago";
    static final String PLAYERS = PREFIX + "players";
    static final String PLAYERS_HINT = PREFIX + "players.hint";
    static final String SEARCH = PREFIX + "search";
    static final String PLAYER_FILTER = PREFIX + "player_filter";
    static final String EMPTY = PREFIX + "empty";
    static final String COUNT = PREFIX + "count";
    static final String CLOSE = PREFIX + "close";
    static final String FILTER_ALL = PREFIX + "filter.all";
    /** Our own map-effect names; Wathe ships none. 地图效果名称由本模组提供，Wathe 未提供。 */
    static final String MAP_EFFECT_PREFIX = PREFIX + "map_effect.";
    /** Wathe's game-mode key convention: gamemode.<ns>.<path>. Wathe 的游戏模式键约定。 */
    static final String GAME_MODE_PREFIX = "gamemode.";

    private ReplayTexts() {
    }

    static String gameModeKey(Identifier id) {
        return GAME_MODE_PREFIX + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    static String mapEffectKey(Identifier id) {
        return MAP_EFFECT_PREFIX + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    static Text gameMode(Identifier id) {
        return Text.translatableWithFallback(gameModeKey(id), readable(id.getPath()));
    }

    static Text mapEffect(Identifier id) {
        return Text.translatableWithFallback(mapEffectKey(id), readable(id.getPath()));
    }

    /** Filter tab key; null is the "all" tab. 筛选标签键；null 表示“全部”。 */
    static String filterKey(@Nullable ReplaySnapshot.Category category) {
        return category == null ? FILTER_ALL : PREFIX + "filter." + category.name().toLowerCase(Locale.ROOT);
    }

    /** "harpy_express_night" -> "Harpy Express Night". 把 id 路径转为可读标题。 */
    static String readable(String path) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder out = new StringBuilder(last.length());
        boolean upper = true;
        for (int i = 0; i < last.length(); i++) {
            char c = last.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != ' ') {
                    out.append(' ');
                }
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString().strip();
    }

    /**
     * The visible text with a leading {@code glyph} (plus one following space) removed, keeping every style. The screen
     * draws category glyphs in its own column, so a line that already starts with the same glyph (the role-change
     * line's "⇄ ") would show it twice.
     * 去掉开头的 {@code glyph}（及其后一个空格）后的文本，保留全部样式。界面在独立的列中绘制分类图标，若行文本本身也以相同
     * 字符开头（例如身份转化行的“⇄ ”），会重复显示。
     */
    static StringVisitable withoutLeadingGlyph(Text text, String glyph) {
        String plain = text.getString();
        if (!plain.startsWith(glyph)) {
            return text;
        }
        int skip = glyph.length() + (plain.startsWith(" ", glyph.length()) ? 1 : 0);
        return new StringVisitable() {
            @Override
            public <T> Optional<T> visit(Visitor<T> visitor) {
                int[] left = {skip};
                return text.visit(part -> {
                    String rest = trim(part, left);
                    return rest.isEmpty() ? Optional.empty() : visitor.accept(rest);
                });
            }

            @Override
            public <T> Optional<T> visit(StyledVisitor<T> visitor, Style style) {
                int[] left = {skip};
                return text.visit((partStyle, part) -> {
                    String rest = trim(part, left);
                    return rest.isEmpty() ? Optional.empty() : visitor.accept(partStyle, rest);
                }, style);
            }
        };
    }

    private static String trim(String part, int[] left) {
        if (left[0] <= 0) {
            return part;
        }
        int cut = Math.min(left[0], part.length());
        left[0] -= cut;
        return part.substring(cut);
    }
}
