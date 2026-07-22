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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final int SPECIAL_ROLE_COLOR = 0x79C7D4;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF5555;
    private static final String SPECIAL_ROLE_HEADER_KEY = "commands.sparkfactionapi.listroles.special";

    private GameSettingsCommandRules() {
    }

    public record RoleListEntry(
            Role role,
            String roleTranslationKey,
            String clickCommand,
            String hoverTranslationKey,
            String enabledTranslationKey,
            int enabledColor,
            boolean enabled,
            boolean toggleable
    ) {
    }

    public record RoleListSection(
            Identifier factionId,
            int headerColor,
            String headerTranslationKey,
            List<RoleListEntry> entries,
            boolean toggleable
    ) {
        public RoleListSection {
            entries = List.copyOf(entries);
        }
    }

    public static MutableText listRolesMessage(
            Collection<Role> roles,
            Collection<Role> specialRoles,
            Predicate<Role> roleEnabled
    ) {
        MutableText message = Text.translatable("commands.wathe.listroles.header").withColor(HEADER_COLOR);
        List<RoleListSection> sections = roleListSections(roles, specialRoles, roleEnabled);

        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            RoleListSection section = sections.get(sectionIndex);
            message.append(sectionIndex == 0 ? "\n" : "\n\n");
            message.append(sectionHeader(section));
            for (RoleListEntry entry : section.entries()) {
                appendRoleLine(message, entry);
            }
        }

        return message;
    }

    /**
     * Groups ordinary roles by their resolved base faction in first-seen order, then appends special
     * roles as a distinct non-toggleable section. SPECIAL_ROLES itself remains owned by Wathe.
     */
    public static List<RoleListSection> roleListSections(
            Collection<Role> roles,
            Collection<Role> specialRoles,
            Predicate<Role> roleEnabled
    ) {
        Map<Identifier, List<RoleListEntry>> groupedEntries = new LinkedHashMap<>();
        Map<Identifier, FactionHeader> factionHeaders = new LinkedHashMap<>();

        for (Role role : roles) {
            if (specialRoles.contains(role)) {
                continue;
            }
            Identifier factionId = SparkFactionApi.resolveBaseFaction(role);
            groupedEntries.computeIfAbsent(factionId, ignored -> new ArrayList<>())
                    .add(toggleableRoleEntry(role, roleEnabled.test(role)));
            factionHeaders.computeIfAbsent(factionId, GameSettingsCommandRules::factionHeader);
        }

        List<RoleListSection> sections = new ArrayList<>();
        groupedEntries.forEach((factionId, entries) -> {
            FactionHeader header = factionHeaders.get(factionId);
            sections.add(new RoleListSection(
                    factionId,
                    header.color(),
                    header.translationKey(),
                    entries,
                    true
            ));
        });

        List<RoleListEntry> specialEntries = specialRoles.stream()
                .map(GameSettingsCommandRules::specialRoleEntry)
                .toList();
        if (!specialEntries.isEmpty()) {
            sections.add(new RoleListSection(
                    null,
                    SPECIAL_ROLE_COLOR,
                    SPECIAL_ROLE_HEADER_KEY,
                    specialEntries,
                    false
            ));
        }

        return List.copyOf(sections);
    }

    public static String roleToggleCommand(String rolePath, boolean enabled) {
        return "/wathe:gameSettings set enableRole " + rolePath + " " + !enabled;
    }

    private static RoleListEntry toggleableRoleEntry(Role role, boolean enabled) {
        String rolePath = role.identifier().getPath();
        return new RoleListEntry(
                role,
                roleTranslationKey(rolePath),
                roleToggleCommand(rolePath, enabled),
                enabled ? "commands.wathe.listroles.click_to_disable" : "commands.wathe.listroles.click_to_enable",
                enabled ? "commands.wathe.listroles.enabled" : "commands.wathe.listroles.disabled",
                enabled ? ENABLED_COLOR : DISABLED_COLOR,
                enabled,
                true
        );
    }

    private static RoleListEntry specialRoleEntry(Role role) {
        return new RoleListEntry(
                role,
                roleTranslationKey(role.identifier().getPath()),
                null,
                null,
                null,
                0,
                false,
                false
        );
    }

    private static String roleTranslationKey(String rolePath) {
        return "announcement.role." + rolePath.replace('/', '.').toLowerCase();
    }

    private static void appendRoleLine(MutableText message, RoleListEntry entry) {
        message.append("\n");
        message.append(roleText(entry));
        if (entry.toggleable()) {
            message.append(enabledText(entry.enabledTranslationKey(), entry.enabledColor()));
        }
    }

    private static Text sectionHeader(RoleListSection section) {
        return Text.translatable(section.headerTranslationKey())
                .withColor(section.headerColor())
                .styled(style -> style.withBold(true).withUnderline(true));
    }

    private static Text roleText(RoleListEntry entry) {
        MutableText roleText = Text.translatable(entry.roleTranslationKey())
                .withColor(entry.role().color());
        if (!entry.toggleable()) {
            return roleText;
        }
        return roleText.styled(style -> style
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

    private static FactionHeader factionHeader(Identifier factionId) {
        FactionDefinition faction = SparkFactionApi.getFaction(factionId).orElse(null);
        return faction == null
                ? new FactionHeader(legacyColor(factionId), legacyFactionKey(factionId))
                : new FactionHeader(faction.color(), faction.translationKeyPrefix());
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

    private record FactionHeader(int color, String translationKey) {
    }
}
