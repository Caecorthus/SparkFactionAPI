package dev.caecorthus.sparkfactionapi.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Registers SparkFactionAPI administrator command modules.
 * 注册 SparkFactionAPI 管理命令模块。
 */
public final class SparkFactionAdminCommands {
    private SparkFactionAdminCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CooldownCommand.register(dispatcher);
        TaskCommand.register(dispatcher);
        SanityCommand.register(dispatcher);
    }
}
