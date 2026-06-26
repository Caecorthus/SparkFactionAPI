package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.command.GameSettingsCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Displays SparkFactionAPI factions in Wathe's role list command.
 * 在 wathe 的角色列表命令中显示 SparkFactionAPI 阵营，避免自定义阵营被误标为原生阵营。
 */
@Mixin(GameSettingsCommand.class)
public abstract class GameSettingsCommandMixin {
    private static final int CIVILIAN_COLOR = 0x36E51B;
    private static final int KILLER_COLOR = 0xC13838;
    private static final int NEUTRAL_COLOR = 0x9F9F9F;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF5555;

    @Inject(method = "listRoles", at = @At("HEAD"), cancellable = true)
    private static void sparkfactionapi$listRoles(
            ServerCommandSource source,
            CallbackInfoReturnable<Integer> cir
    ) {
        MutableText message = Text.translatable("commands.wathe.listroles.header").withColor(0x808080);
        GameWorldComponent game = GameWorldComponent.KEY.get(source.getWorld());

        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) {
                continue;
            }
            message.append("\n");

            Identifier factionId = SparkFactionApi.resolveBaseFaction(role);
            FactionDefinition faction = SparkFactionApi.getFaction(factionId).orElse(null);
            int factionColor = faction == null ? legacyColor(factionId) : faction.color();
            String factionKey = faction == null ? legacyFactionKey(factionId) : faction.translationKeyPrefix();

            message.append(Text.literal("[")
                    .withColor(factionColor)
                    .append(Text.translatable(factionKey))
                    .append(Text.literal("]")));

            boolean enabled = game.isRoleEnabled(role);
            message.append(roleText(role, enabled));
            message.append(enabledText(enabled));
        }

        source.sendMessage(message);
        cir.setReturnValue(1);
    }

    private static Text roleText(Role role, boolean enabled) {
        String rolePath = role.identifier().getPath().replace('/', '.').toLowerCase();
        String command = "/game settings roles " + role.identifier().getPath() + " " + !enabled;
        String hoverKey = enabled
                ? "commands.wathe.listroles.click_to_disable"
                : "commands.wathe.listroles.click_to_enable";
        return Text.translatable("announcement.role." + rolePath)
                .withColor(role.color())
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable(hoverKey)))
                        .withUnderline(true));
    }

    private static Text enabledText(boolean enabled) {
        return Text.literal(" [")
                .append(Text.translatable(enabled
                        ? "commands.wathe.listroles.enabled"
                        : "commands.wathe.listroles.disabled"))
                .append(Text.literal("]"))
                .withColor(enabled ? ENABLED_COLOR : DISABLED_COLOR);
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
