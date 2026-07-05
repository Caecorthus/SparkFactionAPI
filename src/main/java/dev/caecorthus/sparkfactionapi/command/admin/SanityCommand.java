package dev.caecorthus.sparkfactionapi.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

final class SanityCommand {
    private SanityCommand() {
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sparkfactionapi:setSanity")
                .requires(Permissions.require(
                        SparkFactionPermissions.COMMAND_ADMIN,
                        SparkFactionPermissions.DEFAULT_COMMAND_LEVEL
                ))
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(-100.0f, 125.0f))
                        .executes(context -> setSanity(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrThrow()),
                                FloatArgumentType.getFloat(context, "value")
                        ))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(context -> setSanity(
                                        context.getSource(),
                                        EntityArgumentType.getPlayers(context, "targets"),
                                        FloatArgumentType.getFloat(context, "value")
                                )))));
    }

    private static int setSanity(ServerCommandSource source, Collection<ServerPlayerEntity> targets, float sanityPercent) {
        float moodValue = sanityPercentToMood(sanityPercent);
        for (ServerPlayerEntity target : targets) {
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(target);
            mood.setMood(moodValue);
            mood.sync();
        }
        source.sendFeedback(() -> Text.literal(
                "Requested sanity " + sanityPercent + "% for " + targets.size() + " player(s)."
        ), true);
        return targets.size();
    }

    static float sanityPercentToMood(float sanityPercent) {
        return sanityPercent / 100.0f;
    }
}
