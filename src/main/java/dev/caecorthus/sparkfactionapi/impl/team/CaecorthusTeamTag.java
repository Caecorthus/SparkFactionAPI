package dev.caecorthus.sparkfactionapi.impl.team;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.UUID;

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
     * Appends the badge once when {@code uuid} is a team member; any other name is returned untouched.
     * 是团队成员时加一次图标（已经有了就不再加），其他名字原样返回。
     */
    public static Text withTag(UUID uuid, Text name) {
        if (name == null || !isMember(uuid) || name.getString().indexOf(GLYPH) >= 0) {
            return name;
        }
        return name.copy().append(Text.literal(GLYPH_TEXT).styled(style -> style
                .withColor(GLYPH_COLOR)
                .withBold(false)
                .withItalic(false)
                .withObfuscated(false)));
    }

    /**
     * Badge for {@link PlayerEntity#getDisplayName()}. Hidden while a Wathe round is running: disguise roles
     * (Morphling, Puppeteer) sometimes rebuild a target's name from plain text, so a badge tied to the real UUID
     * would give the disguise away. The Tab list always shows it.
     * {@link PlayerEntity#getDisplayName()} 用的标签。Wathe 一局进行中时不加：伪装类角色（变形者、傀儡师）有时用纯文本拼出目标的名字，
     * 跟着真实 UUID 走的图标会暴露伪装。Tab 列表一直显示。
     */
    public static Text withTagOutsideRound(PlayerEntity player, Text name) {
        if (!isMember(player.getUuid())) {
            return name;
        }
        GameWorldComponent game = GameWorldComponent.KEY.getNullable(player.getWorld());
        if (game != null && game.isRunning()) {
            return name;
        }
        return withTag(player.getUuid(), name);
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
