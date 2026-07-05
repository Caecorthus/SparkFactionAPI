package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionWinRulesTest {
    @Test
    void blockOutranksEarlierFactionWin() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");

        FactionWinResult result = FactionWinRules.chooseResult(List.of(
                FactionWinResult.factionWin(witchFaction),
                FactionWinResult.block()
        ));

        assertEquals(FactionWinResult.Type.BLOCK, result.type());
    }

    @Test
    void firstFactionWinIsKeptWhenNoFactionBlocks() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");
        Identifier otherFaction = Identifier.of("sparkwitch", "other");

        FactionWinResult result = FactionWinRules.chooseResult(List.of(
                FactionWinResult.factionWin(witchFaction),
                FactionWinResult.none(),
                FactionWinResult.factionWin(otherFaction)
        ));

        assertEquals(FactionWinResult.Type.FACTION_WIN, result.type());
        assertEquals(witchFaction, result.winningFaction());
    }
}
