package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionWinServiceTest {
    @Test
    void blockOutranksEarlierFactionWin() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");

        FactionWinResult result = FactionWinService.chooseResult(List.of(
                FactionWinResult.factionWin(witchFaction),
                FactionWinResult.block()
        ));

        assertEquals(FactionWinResult.Type.BLOCK, result.type());
    }

    @Test
    void firstFactionWinIsKeptWhenNoFactionBlocks() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");
        Identifier otherFaction = Identifier.of("sparkwitch", "other");

        FactionWinResult result = FactionWinService.chooseResult(List.of(
                FactionWinResult.factionWin(witchFaction),
                FactionWinResult.none(),
                FactionWinResult.factionWin(otherFaction)
        ));

        assertEquals(FactionWinResult.Type.FACTION_WIN, result.type());
        assertEquals(witchFaction, result.winningFaction());
    }
}
