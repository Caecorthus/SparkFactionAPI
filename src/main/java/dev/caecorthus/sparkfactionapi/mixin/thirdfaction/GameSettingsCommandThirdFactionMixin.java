package dev.caecorthus.sparkfactionapi.mixin.thirdfaction;

import com.mojang.brigadier.CommandDispatcher;
import dev.caecorthus.sparkfactionapi.command.settings.ThirdFactionDividendCommand;
import dev.doctor4t.wathe.command.GameSettingsCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wathe seam: once Wathe 1.5.6 {@code GameSettingsCommand.register} has built {@code /wathe:gameSettings}, adds the
 * {@code set roleDividend third_faction <dividend>} option to that same tree (the root keeps Wathe's
 * {@code Permissions.require}). The descriptor holds no Minecraft class, so it is not remapped.
 * Wathe 接缝：在 Wathe 1.5.6 {@code GameSettingsCommand.register} 建好 {@code /wathe:gameSettings} 之后，把
 * {@code set roleDividend third_faction <dividend>} 选项加入同一棵命令树（根节点保留 Wathe 的
 * {@code Permissions.require}）。描述符不含 Minecraft 类，因此不做重映射。
 */
@Mixin(value = GameSettingsCommand.class, remap = false)
public abstract class GameSettingsCommandThirdFactionMixin {
    @Inject(
            method = "register(Lcom/mojang/brigadier/CommandDispatcher;)V",
            at = @At("TAIL"),
            require = 1
    )
    private static void sparkfactionapi$attachThirdFactionDividend(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CallbackInfo ci
    ) {
        ThirdFactionDividendCommand.attach(dispatcher);
    }
}
