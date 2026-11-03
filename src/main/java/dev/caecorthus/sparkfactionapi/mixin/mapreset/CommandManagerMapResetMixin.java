package dev.caecorthus.sparkfactionapi.mixin.mapreset;

import com.mojang.brigadier.ParseResults;
import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapReset;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses map-changing Wathe commands during the lobby map reset. Hooked where every typed command (players, console,
 * command blocks) is executed, so {@code execute ... run} forms are covered without touching Wathe's command lambdas.
 * 大厅地图重置期间拒绝更改地图的 Wathe 命令。挂在所有输入命令（玩家、控制台、命令方块）的执行入口，
 * 因此无需改动 Wathe 的命令 lambda 也能覆盖 {@code execute ... run} 形式。
 */
@Mixin(CommandManager.class)
public abstract class CommandManagerMapResetMixin {
    @Inject(
            method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparkfactionapi$refuseMapCommandsDuringReset(
            ParseResults<ServerCommandSource> parseResults,
            String command,
            CallbackInfo ci
    ) {
        if (LobbyMapReset.refuseCommand(parseResults)) {
            ci.cancel();
        }
    }
}
