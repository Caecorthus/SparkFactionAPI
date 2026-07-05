package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.doctor4t.wathe.game.GameFunctions;

/**
 * Guards when custom faction round-end state may override Wathe round-end rows.
 * 保护自定义阵营结算状态只能在对应的 Wathe 结算写入中接管数据。
 */
public final class FactionRoundEndStateRules {
    private FactionRoundEndStateRules() {
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
