package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;

/**
 * Guards when custom faction round-end state may override Wathe round-end rows.
 * 保护自定义阵营结算状态只能在对应的 Wathe 结算写入中接管数据。
 */
public final class FactionRoundEndStateRules {
    private FactionRoundEndStateRules() {
    }

    /**
     * Wathe fires CheckWinCondition on every STOPPING tick too, but applies a result only while ACTIVE. Custom faction
     * wins are therefore evaluated and recorded only while ACTIVE, so the state set on the winning tick stays frozen
     * for the end screen, the replay and client readers even if a winner leaves or dies during STOPPING.
     * Wathe 在 STOPPING 的每一刻仍会触发 CheckWinCondition，但只在 ACTIVE 时采纳结果。因此自定义阵营胜利只在 ACTIVE 时判定与记录，
     * 即使胜者在 STOPPING 期间离开或死亡，胜利当刻写入的状态也保持冻结，供结算界面、回放与客户端读取。
     */
    public static boolean shouldEvaluateCustomWin(GameWorldComponent.GameStatus gameStatus) {
        return gameStatus == GameWorldComponent.GameStatus.ACTIVE;
    }

    public static boolean shouldWriteCustomRows(
            boolean hasCustomWin,
            boolean pendingCustomWinWrite,
            GameFunctions.WinStatus winStatus
    ) {
        return hasCustomWin
                && pendingCustomWinWrite
                && winStatus == GameFunctions.WinStatus.NEUTRAL;
    }

    public static boolean shouldClearBeforeWatheStatusWrite(
            boolean hasCustomWin,
            boolean pendingCustomWinWrite,
            GameFunctions.WinStatus winStatus
    ) {
        return hasCustomWin && !shouldWriteCustomRows(hasCustomWin, pendingCustomWinWrite, winStatus);
    }

    public static boolean shouldClearBeforeWatheExplicitWinnerWrite(boolean hasCustomWin) {
        return hasCustomWin;
    }
}
