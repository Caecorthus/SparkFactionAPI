package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Shared text keys for custom faction round-end announcements.
 * 自定义阵营结算公告使用的共享文本 key 规则。
 */
public final class FactionRoundEndTextRules {
    private FactionRoundEndTextRules() {
    }

    public static MutableText winTitle(FactionDefinition faction) {
        return Text.translatable(winTitleKey(faction.id())).withColor(faction.color());
    }

    public static String winTitleKey(Identifier factionId) {
        return "announcement.win." + winPhrasePath(factionId);
    }

    public static String winPhraseKey(Identifier factionId) {
        return "game.win." + winPhrasePath(factionId);
    }

    public static String winPhrasePath(Identifier factionId) {
        return factionId.getNamespace() + "." + factionId.getPath().replace('/', '.');
    }
}
