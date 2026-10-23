package dev.caecorthus.sparkfactionapi.api;

/**
 * Decides one custom faction's round result. It is called only while Wathe's game is ACTIVE, never during STOPPING,
 * so side effects here cannot fire after the round has been decided.
 * 判定单个自定义阵营的回合结果。仅在 Wathe 对局处于 ACTIVE 时调用，STOPPING 期间不会调用，因此这里的副作用不会在胜负已定后触发。
 */
@FunctionalInterface
public interface FactionWinCondition {
    FactionWinResult checkWin(FactionWinContext context);

    static FactionWinCondition none() {
        return context -> FactionWinResult.none();
    }
}
