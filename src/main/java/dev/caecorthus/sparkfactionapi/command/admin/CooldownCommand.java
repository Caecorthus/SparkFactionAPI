package dev.caecorthus.sparkfactionapi.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

final class CooldownCommand {
    private CooldownCommand() {
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sparkfactionapi:clearCooldown")
                .requires(Permissions.require(
                        SparkFactionPermissions.COMMAND_ADMIN,
                        SparkFactionPermissions.DEFAULT_COMMAND_LEVEL
                ))
                .executes(context -> clearCooldown(
                        context.getSource(),
                        List.of(context.getSource().getPlayerOrThrow())
                ))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(context -> clearCooldown(
                                context.getSource(),
                                EntityArgumentType.getPlayers(context, "targets")
                        ))));
    }

    private static int clearCooldown(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        int changed = 0;
        for (ServerPlayerEntity target : targets) {
            ItemStack stack = target.getMainHandStack();
            if (stack.isEmpty()) {
                continue;
            }
            target.getItemCooldownManager().remove(stack.getItem());
            changed++;
        }
        int finalChanged = changed;
        source.sendFeedback(() -> Text.literal("Cleared current item cooldown for " + finalChanged + " player(s)."), true);
        return changed;
    }
}
