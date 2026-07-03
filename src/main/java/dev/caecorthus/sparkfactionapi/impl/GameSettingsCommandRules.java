package dev.caecorthus.sparkfactionapi.impl;

/**
 * Pure command-text rules used by the Wathe game-settings mixin.
 * Wathe 游戏设置 mixin 使用的纯命令文本规则。
 */
public final class GameSettingsCommandRules {
    private GameSettingsCommandRules() {
    }

    public static String roleToggleCommand(String rolePath, boolean enabled) {
        return "/wathe:gameSettings set enableRole " + rolePath + " " + !enabled;
    }
}
