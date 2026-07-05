package dev.caecorthus.sparkfactionapi.command.settings;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Owns Wathe game-settings role-list text and click-command rules.
 * 持有 Wathe 游戏设置角色列表文本与点击命令规则。
 */
public final class GameSettingsCommandRules {
    private static final int HEADER_COLOR = 0x808080;
    private static final int CIVILIAN_COLOR = 0x36E51B;
    private static final int KILLER_COLOR = 0xC13838;
    private static final int NEUTRAL_COLOR = 0x9F9F9F;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF5555;

    private GameSettingsCommandRules() {
    }

    public record RoleListEntry(
            Role role,
            int factionColor,
            String factionTranslationKey,
            String roleTranslationKey,
            String clickCommand,
            String hoverTranslationKey,
            String enabledTranslationKey,
            int enabledColor,
            boolean enabled
    ) {
    }

    public static MutableText listRolesMessage(
            Collection<Role> roles,
            Collection<Role> specialRoles,
            Predicate<Role> roleEnabled
    ) {
        MutableText message = Text.translatable("commands.wathe.listroles.header").withColor(HEADER_COLOR);

        for (RoleListEntry entry : roleListEntries(roles, specialRoles, roleEnabled)) {
            appendRoleLine(message, entry);
        }

        return message;
    }

    public static List<RoleListEntry> roleListEntries(
            Collection<Role> roles,
            Collection<Role> specialRoles,
            Predicate<Role> roleEnabled
    ) {
        List<RoleListEntry> entries = new ArrayList<>();
        for (Role role : roles) {
            if (specialRoles.contains(role)) {
                continue;
            }
            entries.add(roleListEntry(role, roleEnabled.test(role)));
        }
        return List.copyOf(entries);
    }

    public static String roleToggleCommand(String rolePath, boolean enabled) {
        return "/wathe:gameSettings set enableRole " + rolePath + " " + !enabled;
    }

    private static RoleListEntry roleListEntry(Role role, boolean enabled) {
        Identifier factionId = SparkFactionApi.resolveBaseFaction(role);
        FactionDefinition faction = SparkFactionApi.getFaction(factionId).orElse(null);
        int factionColor = faction == null ? legacyColor(factionId) : faction.color();
        String factionKey = faction == null ? legacyFactionKey(factionId) : faction.translationKeyPrefix();
        String rolePath = role.identifier().getPath();
        String hoverKey = enabled
                ? "commands.wathe.listroles.click_to_disable"
                : "commands.wathe.listroles.click_to_enable";

        return new RoleListEntry(
                role,
                factionColor,
                factionKey,
                "announcement.role." + rolePath.replace('/', '.').toLowerCase(),
                roleToggleCommand(rolePath, enabled),
                hoverKey,
                enabled ? "commands.wathe.listroles.enabled" : "commands.wathe.listroles.disabled",
                enabled ? ENABLED_COLOR : DISABLED_COLOR,
                enabled
        );
    }

    private static void appendRoleLine(MutableText message, RoleListEntry entry) {
        message.append("\n");
        message.append(factionTag(entry.factionColor(), entry.factionTranslationKey()));
        message.append(roleText(entry));
        message.append(enabledText(entry.enabledTranslationKey(), entry.enabledColor()));
    }

    private static Text factionTag(int color, String translationKey) {
        return Text.literal("[")
                .withColor(color)
                .append(Text.translatable(translationKey))
                .append(Text.literal("]"));
    }

    private static Text roleText(RoleListEntry entry) {
        return Text.translatable(entry.roleTranslationKey())
                .withColor(entry.role().color())
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, entry.clickCommand()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.translatable(entry.hoverTranslationKey())
                        ))
                        .withUnderline(true));
    }

    private static Text enabledText(String translationKey, int color) {
        return Text.literal(" [")
                .append(Text.translatable(translationKey))
                .append(Text.literal("]"))
                .withColor(color);
    }

    private static int legacyColor(Identifier factionId) {
        if (FactionIds.KILLER.equals(factionId)) {
            return KILLER_COLOR;
        }
        if (FactionIds.NEUTRAL.equals(factionId)) {
            return NEUTRAL_COLOR;
        }
        return CIVILIAN_COLOR;
    }

    private static String legacyFactionKey(Identifier factionId) {
        return "faction." + factionId.getNamespace() + "." + factionId.getPath().replace('/', '.');
    }
}
