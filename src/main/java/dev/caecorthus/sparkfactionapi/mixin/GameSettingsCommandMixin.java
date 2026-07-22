package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.command.settings.GameSettingsCommandRules;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.command.GameSettingsCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/GameSettingsCommandMixin");

    @Inject(
            method = "listRoles(Lnet/minecraft/server/command/ServerCommandSource;)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void sparkfactionapi$listRoles(
            ServerCommandSource source,
            CallbackInfoReturnable<Integer> cir
    ) {
        try {
            LOGGER.info(
                    "SparkFactionAPI listRoles diagnostic start: world={}, totalRoles={}, specialRoles={}",
                    source.getWorld().getRegistryKey().getValue(),
                    WatheRoles.ROLES.size(),
                    WatheRoles.SPECIAL_ROLES.size()
            );
            GameWorldComponent game = GameWorldComponent.KEY.get(source.getWorld());
            MutableText message = GameSettingsCommandRules.listRolesMessage(
                    WatheRoles.ROLES,
                    WatheRoles.SPECIAL_ROLES,
                    game::isRoleEnabled
            );

            source.sendMessage(message);
            cir.setReturnValue(1);
        } catch (RuntimeException exception) {
            // Keep command failures visible without putting text rules back into the Mixin Adapter.
            // 保留命令失败诊断，但不把文本规则重新塞回 Mixin 适配器。
            LOGGER.error(
                    "SparkFactionAPI listRoles failed while building or sending the role list. "
                            + "SparkFactionAPI listRoles 在构建或发送角色列表时失败。",
                    exception
            );
            throw exception;
        }
    }
}
