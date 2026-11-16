package dev.caecorthus.sparkfactionapi.command.settings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.caecorthus.sparkfactionapi.component.SparkFactionThirdFactionComponent;
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionDrawRules;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code /wathe:gameSettings set roleDividend third_faction <dividend>}: sets the server-wide third-faction dividend
 * (member cap = floor(players / dividend), from the next round). The node is grafted onto Wathe's own
 * {@code roleDividend} node, so Wathe's permission requirement on {@code wathe:gameSettings} still guards it; the
 * argument floor (3) mirrors Wathe's vigilante/neutral options.
 * {@code /wathe:gameSettings set roleDividend third_faction <dividend>}：设置全服第三方阵营除数（成员上限 =
 * floor(总人数 / 除数)，从下一局生效）。该节点挂在 Wathe 自己的 {@code roleDividend} 节点下，因此仍受
 * {@code wathe:gameSettings} 上 Wathe 权限要求的保护；参数下限 3 与 Wathe 的义警/中立选项一致。
 */
public final class ThirdFactionDividendCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/ThirdFactionDividendCommand");
    public static final String ROOT = "wathe:gameSettings";
    public static final String SET = "set";
    public static final String ROLE_DIVIDEND = "roleDividend";
    public static final String LITERAL = "third_faction";
    public static final String ARGUMENT = "dividend";
    private static final String SET_KEY = "commands.sparkfactionapi.thirdfaction.dividend.set";

    private ThirdFactionDividendCommand() {
    }

    /**
     * Adds {@code third_faction <dividend>} under Wathe's {@code wathe:gameSettings set roleDividend}. Fails closed:
     * when Wathe's tree lacks a node it logs one WARN and adds nothing.
     * 在 Wathe 的 {@code wathe:gameSettings set roleDividend} 下添加 {@code third_faction <dividend>}。失败即关闭：
     * Wathe 命令树缺少节点时记录一条 WARN 并且不添加任何内容。
     *
     * @return whether the node was attached 是否已挂载
     */
    public static boolean attach(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> roleDividend = child(child(child(dispatcher.getRoot(), ROOT), SET), ROLE_DIVIDEND);
        if (roleDividend == null) {
            LOGGER.warn("Wathe's /{} {} {} node is missing; /{} {} {} {} is not available. "
                            + "缺少 Wathe 的 roleDividend 节点，第三方阵营除数命令不可用。",
                    ROOT, SET, ROLE_DIVIDEND, ROOT, SET, ROLE_DIVIDEND, LITERAL);
            return false;
        }
        roleDividend.addChild(node());
        return true;
    }

    public static LiteralCommandNode<ServerCommandSource> node() {
        return CommandManager.literal(LITERAL)
                .then(CommandManager.argument(ARGUMENT, IntegerArgumentType.integer(ThirdFactionDrawRules.MIN_DIVIDEND))
                        .executes(context -> setDividend(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, ARGUMENT)
                        )))
                .build();
    }

    private static int setDividend(ServerCommandSource source, int dividend) {
        SparkFactionThirdFactionComponent settings = SparkFactionThirdFactionComponent.of(source.getServer());
        settings.setDividend(dividend);
        int stored = settings.settings().dividend();
        source.sendFeedback(() -> Text.translatableWithFallback(
                SET_KEY,
                "Third-faction dividend set to %s (member cap = players / %s)",
                stored,
                stored
        ), true);
        return Command.SINGLE_SUCCESS;
    }

    private static @Nullable CommandNode<ServerCommandSource> child(
            @Nullable CommandNode<ServerCommandSource> parent,
            String name
    ) {
        return parent == null ? null : parent.getChild(name);
    }
}
