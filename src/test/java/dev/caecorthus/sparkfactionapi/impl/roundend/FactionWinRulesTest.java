package dev.caecorthus.sparkfactionapi.impl.roundend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import java.util.List;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class FactionWinRulesTest {
    @Test
    void blockOutranksEarlierFactionWin() {
        FactionWinResult result = FactionWinRules.chooseResult(List.of(
                FactionWinResult.factionWin(Identifier.of("sparkwitch", "witch")),
                FactionWinResult.block()
        ));

        assertEquals(FactionWinResult.Type.BLOCK, result.type());
    }

    @Test
    void firstFactionWinIsKeptWhenNoFactionBlocks() {
        Identifier firstFaction = Identifier.of("sparkwitch", "witch");
        FactionWinResult result = FactionWinRules.chooseResult(List.of(
                FactionWinResult.factionWin(firstFaction),
                FactionWinResult.none(),
                FactionWinResult.factionWin(Identifier.of("sparkwitch", "other"))
        ));

        assertEquals(FactionWinResult.Type.FACTION_WIN, result.type());
        assertEquals(firstFaction, result.winningFaction());
    }
}
