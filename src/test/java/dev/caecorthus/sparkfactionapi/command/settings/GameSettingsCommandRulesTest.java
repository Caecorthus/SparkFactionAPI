package dev.caecorthus.sparkfactionapi.command.settings;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsCommandRulesTest {

    @Test
    void groupsOrdinaryRolesByResolvedFactionInFirstSeenOrder() {
        Role civilianOne = role("civilian_one", 0x111111, true, false);
        Role killer = role("killer_one", 0x222222, false, true);
        Role civilianTwo = role("civilian_two", 0x333333, true, false);

        List<GameSettingsCommandRules.RoleListSection> sections = GameSettingsCommandRules.roleListSections(
                List.of(civilianOne, killer, civilianTwo),
                List.of(),
                ignored -> true
        );

        assertEquals(2, sections.size());
        assertEquals(Identifier.of("wathe", "civilian"), sections.get(0).factionId());
        assertEquals(List.of(civilianOne, civilianTwo), sections.get(0).entries().stream()
                .map(GameSettingsCommandRules.RoleListEntry::role)
                .toList());
        assertEquals(Identifier.of("wathe", "killer"), sections.get(1).factionId());
        assertEquals(List.of(killer), sections.get(1).entries().stream()
                .map(GameSettingsCommandRules.RoleListEntry::role)
                .toList());
    }

    @Test
    void specialRolesRemainVisibleInASeparateNonToggleableSection() {
        Role ordinary = role("ordinary", 0x111111, true, false);
        Role wraith = role("wraith", 0x79C7D4, false, false);

        List<GameSettingsCommandRules.RoleListSection> sections = GameSettingsCommandRules.roleListSections(
                List.of(ordinary, wraith),
                List.of(wraith),
                ignored -> true
        );

        assertEquals(2, sections.size());
        assertEquals(List.of(ordinary), sections.get(0).entries().stream()
                .map(GameSettingsCommandRules.RoleListEntry::role)
                .toList());
        GameSettingsCommandRules.RoleListSection specialSection = sections.get(1);
        assertEquals("commands.sparkfactionapi.listroles.special", specialSection.headerTranslationKey());
        assertFalse(specialSection.toggleable());
        assertNull(specialSection.factionId());
        assertEquals(List.of(wraith), specialSection.entries().stream()
                .map(GameSettingsCommandRules.RoleListEntry::role)
                .toList());
        assertNull(specialSection.entries().getFirst().clickCommand());
    }

    @Test
    void ordinaryEntriesPreserveClickHoverAndEnabledState() {
        Role enabled = role("enabled", 0x123456, true, false);
        Role disabled = role("disabled", 0x654321, true, false);

        List<GameSettingsCommandRules.RoleListEntry> entries = GameSettingsCommandRules.roleListSections(
                List.of(enabled, disabled),
                List.of(),
                role -> role == enabled
        ).getFirst().entries();

        assertTrue(entries.get(0).toggleable());
        assertTrue(entries.get(0).enabled());
        assertEquals("/wathe:gameSettings set enableRole enabled false", entries.get(0).clickCommand());
        assertEquals("commands.wathe.listroles.click_to_disable", entries.get(0).hoverTranslationKey());
        assertEquals("commands.wathe.listroles.enabled", entries.get(0).enabledTranslationKey());

        assertFalse(entries.get(1).enabled());
        assertEquals("/wathe:gameSettings set enableRole disabled true", entries.get(1).clickCommand());
        assertEquals("commands.wathe.listroles.click_to_enable", entries.get(1).hoverTranslationKey());
        assertEquals("commands.wathe.listroles.disabled", entries.get(1).enabledTranslationKey());
    }

    @Test
    void renderedSpecialSectionHasStyledHeaderAndNoToggleState() {
        Role wraith = role("wraith", 0x79C7D4, false, false);

        MutableText message = GameSettingsCommandRules.listRolesMessage(
                List.of(wraith),
                List.of(wraith),
                ignored -> true
        );
        List<Text> siblings = message.getSiblings();

        assertEquals(4, siblings.size());
        assertEquals("\n", siblings.get(0).getLiteralString());
        assertHeader(siblings.get(1), "commands.sparkfactionapi.listroles.special", 0x79C7D4);
        assertEquals("\n", siblings.get(2).getLiteralString());
        Text specialRole = siblings.get(3);
        assertEquals("announcement.role.wraith", translationKey(specialRole));
        assertEquals(wraith.color(), specialRole.getStyle().getColor().getRgb());
        assertNull(specialRole.getStyle().getClickEvent());
        assertNull(specialRole.getStyle().getHoverEvent());
        assertFalse(specialRole.getStyle().isUnderlined());
    }

    private static void assertHeader(Text text, String key, int color) {
        assertEquals(key, translationKey(text));
        assertEquals(color, text.getStyle().getColor().getRgb());
        assertTrue(text.getStyle().isBold());
        assertTrue(text.getStyle().isUnderlined());
    }

    private static String translationKey(Text text) {
        return ((TranslatableTextContent) text.getContent()).getKey();
    }

    private static Role role(String path, int color, boolean innocent, boolean killer) {
        return new Role(
                Identifier.of("test", path),
                color,
                innocent,
                killer,
                Role.MoodType.NONE,
                0,
                false
        );
    }
}
