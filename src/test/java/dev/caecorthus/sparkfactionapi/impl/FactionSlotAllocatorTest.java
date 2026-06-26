package dev.caecorthus.sparkfactionapi.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionSlotAllocatorTest {
    @Test
    void shortageAllocationIgnoresPriorityAndGivesEachFactionAFairPass() {
        List<Integer> grants = FactionSlotAllocator.allocateShortage(List.of(2, 2, 2), 2, new Random(1));

        assertEquals(2, grants.stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, grants.stream().filter(value -> value == 1).count());
        assertEquals(1, grants.stream().filter(value -> value == 0).count());
    }

    @Test
    void shortageAllocationRespectsRequestedCaps() {
        List<Integer> grants = FactionSlotAllocator.allocateShortage(List.of(1, 3), 3, new Random(2));

        assertEquals(3, grants.stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, grants.get(0));
        assertEquals(2, grants.get(1));
    }
}
