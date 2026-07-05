package dev.caecorthus.sparkfactionapi.impl.assignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pure custom-faction slot allocation helper.
 * 自定义阵营名额分配的纯逻辑辅助类，便于用普通 JUnit 覆盖。
 */
final class FactionSlotAllocator {
    private FactionSlotAllocator() {
    }

    static List<Integer> allocateShortage(List<Integer> desiredSlots, int availableSlots, Random random) {
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < desiredSlots.size(); index++) {
            if (desiredSlots.get(index) > 0) {
                order.add(index);
            }
        }
        shuffle(order, random);

        Map<Integer, Integer> grants = new LinkedHashMap<>();
        for (Integer index : order) {
            grants.put(index, 0);
        }

        int granted = 0;
        while (granted < availableSlots) {
            boolean madeProgress = false;
            for (Integer index : order) {
                int current = grants.get(index);
                if (current < desiredSlots.get(index)) {
                    grants.put(index, current + 1);
                    granted++;
                    madeProgress = true;
                    if (granted >= availableSlots) {
                        break;
                    }
                }
            }
            if (!madeProgress) {
                break;
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < desiredSlots.size(); index++) {
            result.add(grants.getOrDefault(index, 0));
        }
        return result;
    }

    private static void shuffle(List<Integer> order, Random random) {
        for (int index = order.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            Integer value = order.get(index);
            order.set(index, order.get(swap));
            order.set(swap, value);
        }
    }
}
