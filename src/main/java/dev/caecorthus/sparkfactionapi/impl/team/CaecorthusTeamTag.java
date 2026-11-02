package dev.caecorthus.sparkfactionapi.impl.team;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Caecorthus team badge: a bitmap-font glyph appended after team members' names, the way Ratatouille marks its
 * supporters. The glyph is registered in {@code assets/minecraft/font/default.json}; the member list comes from the
 * website through {@link CaecorthusTeamSync}.
 * Caecorthus 团队标签：在团队成员的名字后面加一个位图字体图标，做法同 Ratatouille 的赞助者图标。
 * 字符登记在 {@code assets/minecraft/font/default.json}，名单由 {@link CaecorthusTeamSync} 从网站拉取。
 */
public final class CaecorthusTeamTag {
    /**
     * Private-use glyph of the badge. E780 is Ratatouille's supporter icon, E781 Wathe's coin, E782/E783 SparkWitch.
     * 标签用的私用区字符。E780 是 Ratatouille 的赞助者图标，E781 是 Wathe 的金币，E782、E783 是 SparkWitch 的。
     */
    public static final char GLYPH = '';
    public static final char RATATOUILLE_SUPPORTER_GLYPH = '';

    private static final String GLYPH_TEXT = String.valueOf(GLYPH);
    private static final String RATATOUILLE_SUPPORTER_TEXT = String.valueOf(RATATOUILLE_SUPPORTER_GLYPH);
    // White, never bold/italic/obfuscated: the badge must not pick up the name's own colour or formatting.
    // 固定白色、不加粗不斜体不乱码：图标不跟着名字的颜色和格式走。
    private static final int GLYPH_COLOR = 0xFFFFFF;

    private static volatile Set<UUID> members = Set.of();

    private CaecorthusTeamTag() {
    }

    public static boolean isMember(UUID uuid) {
        return uuid != null && members.contains(uuid);
    }

    static void setMembers(Set<UUID> next) {
        members = Set.copyOf(next);
    }

    /**
     * Appends the badge once when {@code uuid} is a team member; any other name is returned untouched. The badge
     * goes before a trailing Ratatouille supporter icon, and Ratatouille appends after it when it runs later, so a
     * member who is also a supporter always reads name, badge, supporter icon whatever order the mixins apply in.
     * 是团队成员时加一次图标（已经有了就不再加），其他名字原样返回。图标放在结尾的 Ratatouille 赞助者图标之前；
     * Ratatouille 后执行时会接在本图标后面。所以既是成员又是赞助者时，不管 mixin 先后，顺序都是名字、本图标、赞助者图标。
     */
    public static Text withTag(UUID uuid, Text name) {
        if (name == null || !isMember(uuid) || name.getString().indexOf(GLYPH) >= 0) {
            return name;
        }
        MutableText tagged = name.copy();
        List<Text> siblings = tagged.getSiblings();
        int at = siblings.size();
        if (at > 0 && RATATOUILLE_SUPPORTER_TEXT.equals(siblings.get(at - 1).getString())) {
            at--;
        }
        siblings.add(at, Text.literal(GLYPH_TEXT).styled(style -> style
                .withColor(GLYPH_COLOR)
                .withBold(false)
                .withItalic(false)
                .withObfuscated(false)));
        return tagged;
    }

    /**
     * Badge for a name as it is shown, which may be a disguise: Morphling, Coroner and other disguises put another
     * player's name on screen, sometimes as plain text without that player's badge. The shown name (without badge
     * glyphs) is looked up with {@code playerByName}; the badge then matches that player, so a disguise carries the
     * badge of whoever it imitates and never the disguiser's own. A name that matches no player, and any obfuscated
     * text (psycho and hallucination placeholders), is left as it is.
     * 按屏幕上显示的名字加标签，这个名字可能是伪装：变形者、验尸官等伪装会把别人的名字显示出来，有时是不带标签的纯文本。
     * 用 {@code playerByName} 查出显示的名字（去掉图标）是谁，标签跟着这个人走：伪装成谁就带谁的标签，不会带出伪装者自己的。
     * 查不到人的名字，以及带乱码样式的文字（疯魔、幻觉的占位名），原样返回。
     */
    public static Text withTagForShownName(Text shown, Function<String, UUID> playerByName) {
        if (shown == null || isObfuscated(shown)) {
            return shown;
        }
        String text = shown.getString();
        UUID shownPlayer = playerByName.apply(stripGlyphs(text));
        if (shownPlayer == null) {
            return shown;
        }
        if (isMember(shownPlayer)) {
            return withTag(shownPlayer, shown);
        }
        if (text.indexOf(GLYPH) < 0) {
            return shown;
        }
        MutableText untagged = shown.copy();
        untagged.getSiblings().removeIf(sibling -> GLYPH_TEXT.equals(sibling.getString()));
        return untagged;
    }

    private static boolean isObfuscated(Text text) {
        return text.visit((style, part) -> style.isObfuscated() && !part.isEmpty() ? Optional.of(Boolean.TRUE)
                : Optional.empty(), Style.EMPTY).isPresent();
    }

    /**
     * Removes this badge and Ratatouille's from a plain-text name (Wathe only strips a trailing E780 from letters).
     * 从纯文本名字里去掉本标签和 Ratatouille 的图标（Wathe 给信件去图标时只认结尾的一个 E780）。
     */
    public static String stripGlyphs(String name) {
        if (name == null || (name.indexOf(GLYPH) < 0 && name.indexOf(RATATOUILLE_SUPPORTER_GLYPH) < 0)) {
            return name;
        }
        StringBuilder stripped = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != GLYPH && c != RATATOUILLE_SUPPORTER_GLYPH) {
                stripped.append(c);
            }
        }
        return stripped.toString();
    }
}
