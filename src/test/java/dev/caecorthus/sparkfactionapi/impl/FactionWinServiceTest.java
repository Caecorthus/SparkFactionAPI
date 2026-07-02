package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionWinResult;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    @Test
    void customFactionWinMarksAllRoundMembersAsWinners() {
        Identifier witchFaction = Identifier.of("sparkwitch", "witch");
        Identifier civilianFaction = Identifier.of("wathe", "civilian");
        UUID livingGrandWitch = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID deadAccomplice = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID passenger = UUID.fromString("00000000-0000-0000-0000-000000000103");
        Map<UUID, Identifier> roundFactions = new LinkedHashMap<>();
        roundFactions.put(livingGrandWitch, witchFaction);
        roundFactions.put(deadAccomplice, witchFaction);
        roundFactions.put(passenger, civilianFaction);

        Set<UUID> winners = FactionWinService.collectFactionWinners(
                roundFactions.keySet(),
                roundFactions::get,
                witchFaction
        );

        assertEquals(Set.of(livingGrandWitch, deadAccomplice), winners);
    }
}
