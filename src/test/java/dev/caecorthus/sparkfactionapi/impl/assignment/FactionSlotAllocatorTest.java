package dev.caecorthus.sparkfactionapi.impl.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FactionSlotAllocatorTest {
    @Test
    void shortageAllocationGivesEachFactionAtMostOneSlotPerPass() {
        List<Integer> grants = FactionSlotAllocator.allocateShortage(List.of(2, 2, 2), 2, new Random(1));

        assertEquals(2, grants.stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, grants.stream().filter(value -> value == 1).count());
        assertEquals(1, grants.stream().filter(value -> value == 0).count());
    }

    @Test
    void shortageAllocationRespectsRequestedCaps() {
        List<Integer> grants = FactionSlotAllocator.allocateShortage(List.of(1, 3), 3, new Random(2));

        assertEquals(List.of(1, 2), grants);
    }
}
