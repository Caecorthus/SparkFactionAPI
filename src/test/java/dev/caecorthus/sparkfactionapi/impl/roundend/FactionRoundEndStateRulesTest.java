package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.doctor4t.wathe.game.GameFunctions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionRoundEndStateRulesTest {
    @Test
    void onlyPendingCustomNeutralStatusWriteUsesCustomRows() {
        assertTrue(FactionRoundEndStateRules.shouldWriteCustomRows(
                true,
                true,
                GameFunctions.WinStatus.NEUTRAL
        ));

        assertFalse(FactionRoundEndStateRules.shouldWriteCustomRows(
                true,
                false,
                GameFunctions.WinStatus.NEUTRAL
        ));
        assertFalse(FactionRoundEndStateRules.shouldWriteCustomRows(
                true,
                true,
                GameFunctions.WinStatus.KILLERS
        ));
        assertFalse(FactionRoundEndStateRules.shouldWriteCustomRows(
                false,
                true,
                GameFunctions.WinStatus.NEUTRAL
        ));
    }

    @Test
    void staleCustomStateClearsBeforeNonCustomStatusWrite() {
        assertTrue(FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite(
                true,
                false,
                GameFunctions.WinStatus.NEUTRAL
        ));
        assertTrue(FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite(
                true,
                true,
                GameFunctions.WinStatus.KILLERS
        ));

        assertFalse(FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite(
                true,
                true,
                GameFunctions.WinStatus.NEUTRAL
        ));
        assertFalse(FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite(
                false,
                false,
                GameFunctions.WinStatus.PASSENGERS
        ));
    }

    @Test
    void staleCustomStateClearsBeforeExplicitNeutralWinnerWrite() {
        assertTrue(FactionRoundEndStateRules.shouldClearBeforeWatheExplicitWinnerWrite(true));
        assertFalse(FactionRoundEndStateRules.shouldClearBeforeWatheExplicitWinnerWrite(false));
    }
}
