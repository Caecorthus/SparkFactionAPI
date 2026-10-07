package dev.caecorthus.sparkfactionapi.command.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.caecorthus.sparkfactionapi.component.SparkFactionAntiFastRoundComponent;
import dev.caecorthus.sparkfactionapi.impl.antifastround.AntiFastRound;
import dev.caecorthus.sparkfactionapi.impl.antifastround.AntiFastRoundRules;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * {@code /sparkfactionapi:antifastround [<enabled> [<seconds>]]}: shows or changes the server-wide safe-time settings.
 * New settings apply from the next round start; disabling also ends a running window. Feedback uses translation keys
 * with English fallbacks so dedicated-server consoles stay readable.
 * {@code /sparkfactionapi:antifastround [<enabled> [<seconds>]]}：查看或修改全服的开局安全时间设置。
 * 新设置从下一局开始生效；关闭时同时结束正在进行的窗口。反馈使用带英文回退的翻译键，保证专用服务器控制台可读。
 */
final class AntiFastRoundCommand {
    private static final String KEY_PREFIX = "commands.sparkfactionapi.antifastround.";

    private AntiFastRoundCommand() {
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sparkfactionapi:antifastround")
                .requires(Permissions.require(
                        SparkFactionPermissions.COMMAND_ADMIN,
                        SparkFactionPermissions.DEFAULT_COMMAND_LEVEL
                ))
                .executes(context -> showStatus(context.getSource()))
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            return update(
                                    source,
                                    BoolArgumentType.getBool(context, "enabled"),
                                    AntiFastRound.settings(source.getServer()).getSeconds()
                            );
                        })
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(
                                        AntiFastRoundRules.MIN_SECONDS,
                                        AntiFastRoundRules.MAX_SECONDS
                                ))
                                .executes(context -> update(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled"),
                                        IntegerArgumentType.getInteger(context, "seconds")
                                )))));
    }

    private static int showStatus(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        SparkFactionAntiFastRoundComponent settings = AntiFastRound.settings(server);
        int seconds = settings.getSeconds();
        if (settings.isEnabled()) {
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "status.enabled",
                    "Anti-fast-round is enabled: %s s of safe time at each round start",
                    seconds
            ), false);
        } else {
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "status.disabled",
                    "Anti-fast-round is disabled (safe time setting: %s s)",
                    seconds
            ), false);
        }
        if (settings.hasOpenWindow()) {
            int remaining = remainingWindowSeconds(server, settings);
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "status.window",
                    "A safe-time window is running now (%s s left)",
                    remaining
            ), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int update(ServerCommandSource source, boolean enabled, int seconds) {
        MinecraftServer server = source.getServer();
        SparkFactionAntiFastRoundComponent settings = AntiFastRound.settings(server);
        boolean windowWasOpen = settings.hasOpenWindow();
        AntiFastRound.applySettings(server, enabled, seconds);
        // Read back the stored (clamped) value instead of echoing the argument.
        // 回读已保存（已限幅）的值，而不是直接回显参数。
        int storedSeconds = settings.getSeconds();
        if (enabled) {
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "enabled",
                    "Anti-fast-round enabled: %s s of safe time from the next round start",
                    storedSeconds
            ), true);
        } else if (windowWasOpen) {
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "disabled.window_ended",
                    "Anti-fast-round disabled (safe time setting: %s s); the running safe time ended immediately",
                    storedSeconds
            ), true);
        } else {
            source.sendFeedback(() -> Text.translatableWithFallback(
                    KEY_PREFIX + "disabled",
                    "Anti-fast-round disabled (safe time setting: %s s)",
                    storedSeconds
            ), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int remainingWindowSeconds(MinecraftServer server, SparkFactionAntiFastRoundComponent settings) {
        ServerWorld world = settings.getWindowWorld() == null ? null : server.getWorld(settings.getWindowWorld());
        if (world == null) {
            world = server.getOverworld();
        }
        return AntiFastRoundRules.remainingSeconds(world.getTime(), settings.getWindowEnd());
    }
}
