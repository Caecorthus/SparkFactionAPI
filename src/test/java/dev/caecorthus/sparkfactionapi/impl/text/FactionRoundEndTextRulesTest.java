package dev.caecorthus.sparkfactionapi.impl.text;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FactionRoundEndTextRulesTest {
    @Test
    void customFactionWinTitleUsesFactionKeyAndColor() {
        FactionDefinition faction = FactionDefinition.builder(Identifier.of("sparkwitch", "witch"))
                .color(0xE9D5F0)
                .build();

        MutableText title = FactionRoundEndTextRules.winTitle(faction);
        TranslatableTextContent content = assertInstanceOf(TranslatableTextContent.class, title.getContent());

        assertEquals("announcement.win.sparkwitch.witch", content.getKey());
        assertEquals(0xE9D5F0, title.getStyle().getColor().getRgb());
    }

    @Test
    void customFactionWinPhraseUsesFactionPath() {
        Identifier factionId = Identifier.of("sparkwitch", "witch");

        assertEquals("sparkwitch.witch", FactionRoundEndTextRules.winPhrasePath(factionId));
        assertEquals("game.win.sparkwitch.witch", FactionRoundEndTextRules.winPhraseKey(factionId));
    }
}
