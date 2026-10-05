package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Pure choice of the replay header's result title, mirroring Wathe 1.5.6 {@code RoundTextRenderer#renderHud}: a
 * FactionAPI custom win shows {@code FactionRoundEndTextRules#winTitle}; another NEUTRAL win shows the first winner
 * row's {@code announcement.win.<rolePath>}; PASSENGERS/TIME and KILLERS show the civilian/killer titles; LOOSE_END
 * passes the winner name. Wathe colours these titles with the same RGB as the matching {@code Role}.
 * 回放头部结果标题的纯规则，与 Wathe 1.5.6 {@code RoundTextRenderer#renderHud} 一致：FactionAPI 自定义胜利显示
 * {@code FactionRoundEndTextRules#winTitle}；其他 NEUTRAL 胜利显示首个胜者行的 {@code announcement.win.<身份路径>}；
 * PASSENGERS/TIME 与 KILLERS 显示平民/杀手标题；LOOSE_END 附带胜者名字。Wathe 标题颜色与对应 {@code Role} 的 RGB 相同。
 */
public final class ReplayOutcomeRules {
    static final Identifier CIVILIAN = Identifier.of("wathe", "civilian");
    static final Identifier KILLER = Identifier.of("wathe", "killer");
    static final Identifier LOOSE_END = Identifier.of("wathe", "loose_end");
    private static final String WIN_TITLE_PREFIX = "announcement.win.";

    private ReplayOutcomeRules() {
    }

    /**
     * @param shown            round-end data is current and the mode shows a title (Wathe hides it in Discovery);
     *                         结算数据属于本局且该模式显示标题（Wathe 在 Discovery 中隐藏）
     * @param customWinTitle   FactionAPI custom-win title, used only for NEUTRAL; 自定义阵营胜利标题，仅用于 NEUTRAL
     * @param firstWinnerRole  role of the first winner row, used for other NEUTRAL wins; 首个胜者行的身份
     */
    public static Optional<Text> outcome(
            boolean shown,
            GameFunctions.WinStatus status,
            @Nullable Text customWinTitle,
            @Nullable Identifier firstWinnerRole,
            ToIntFunction<Identifier> roleColor,
            String looseEndWinner
    ) {
        if (!shown) {
            return Optional.empty();
        }
        return switch (status) {
            case NONE -> Optional.empty();
            case PASSENGERS, TIME -> Optional.of(roleTitle(CIVILIAN, roleColor));
            case KILLERS -> Optional.of(roleTitle(KILLER, roleColor));
            case LOOSE_END -> Optional.of(Text.translatable(
                    WIN_TITLE_PREFIX + LOOSE_END.getPath(),
                    Text.literal(looseEndWinner)
            ).withColor(roleColor.applyAsInt(LOOSE_END)));
            case NEUTRAL -> customWinTitle != null
                    ? Optional.of(customWinTitle)
                    : Optional.ofNullable(firstWinnerRole).map(role -> roleTitle(role, roleColor));
        };
    }

    private static MutableText roleTitle(Identifier role, ToIntFunction<Identifier> roleColor) {
        return Text.translatable(WIN_TITLE_PREFIX + role.getPath()).withColor(roleColor.applyAsInt(role));
    }
}
