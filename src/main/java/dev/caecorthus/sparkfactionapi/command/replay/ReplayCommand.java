package dev.caecorthus.sparkfactionapi.command.replay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.caecorthus.sparkfactionapi.impl.replay.ReplayArchive;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * {@code /replay}: resends the latest end-of-round replay to the calling player. Players only, no permission node.
 * {@code /replay}：向执行者重发最近一局的回放。仅限玩家执行，无权限节点。
 */
public final class ReplayCommand {
    public static final String LITERAL = "replay";

    private ReplayCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(LITERAL)
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(context -> resend(context.getSource())));
    }

    private static int resend(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<Text> lines = ReplayArchive.latest();
        if (lines.isEmpty()) {
            source.sendError(Text.translatable("commands.sparkfactionapi.replay.none"));
            return 0;
        }
        for (Text line : lines) {
            player.sendMessage(line, false);
        }
        return lines.size();
    }
}
