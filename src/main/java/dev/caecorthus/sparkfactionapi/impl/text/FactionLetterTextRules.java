package dev.caecorthus.sparkfactionapi.impl.text;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

/**
 * Owns custom faction names used by Wathe letter lore.
 * 持有 Wathe 信件 lore 中使用的自定义阵营名称规则。
 */
public final class FactionLetterTextRules {
    private FactionLetterTextRules() {
    }

    public static String letterFactionName(Role role, String fallback) {
        Identifier factionId = SparkFactionApi.resolveBaseFaction(role);
        FactionDefinition definition = SparkFactionApi.getFaction(factionId).orElse(null);
        if (definition == null || factionId.getNamespace().equals("wathe")) {
            return fallback;
        }
        return factionId.getNamespace() + "." + factionId.getPath().replace('/', '.');
    }
}
