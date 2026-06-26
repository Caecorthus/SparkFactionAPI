package dev.caecorthus.sparkfactionapi.api;

@FunctionalInterface
public interface FactionWinCondition {
    FactionWinResult checkWin(FactionWinContext context);

    static FactionWinCondition none() {
        return context -> FactionWinResult.none();
    }
}
