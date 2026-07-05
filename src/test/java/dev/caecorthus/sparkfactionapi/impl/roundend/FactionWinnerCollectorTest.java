package dev.caecorthus.sparkfactionapi.impl.roundend;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionWinnerCollectorTest {
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

        Set<UUID> winners = FactionWinnerCollector.collectFactionWinners(
                roundFactions.keySet(),
                roundFactions::get,
                witchFaction
        );

        assertEquals(Set.of(livingGrandWitch, deadAccomplice), winners);
    }
}
