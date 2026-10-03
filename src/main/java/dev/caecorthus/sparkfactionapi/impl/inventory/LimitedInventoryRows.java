package dev.caecorthus.sparkfactionapi.impl.inventory;

import java.util.function.IntPredicate;

/**
 * Slot rules of the two-row limited inventory shown during a Wathe round: the hotbar (0-8) plus vanilla's bottom main
 * row (27-35), which vanilla already draws directly above the hotbar. Main 9-26 stay hidden; Spark add-ons park
 * role items there (filling upward from 9), so they are deliberately not exposed.
 * Wathe 对局中两行受限物品栏的槽位规则：快捷栏（0-8）加原版紧贴快捷栏上方的主背包底行（27-35）。
 * 主背包 9-26 保持隐藏；Spark 附属模组会把职业物品从 9 往后藏在这里，因此刻意不暴露。
 */
public final class LimitedInventoryRows {
    public static final int ROW_SIZE = 9;
    public static final int SECOND_ROW_START = 27;
    public static final int MAIN_SIZE = 36;

    private static final int[] EMPTY_SLOT_ORDER = emptySlotOrder();

    private LimitedInventoryRows() {
    }

    public static boolean isSecondRowIndex(int index) {
        return index >= SECOND_ROW_START && index < SECOND_ROW_START + ROW_SIZE;
    }

    /**
     * Visible slots first (hotbar, then second row), then hidden 9-26, so overflow keeps vanilla capacity instead of
     * being lost or dropped; {@code -1} when full. A non-vanilla main size falls back to vanilla's ascending scan.
     * 先可见槽位（快捷栏、第二行），再隐藏的 9-26，溢出时保持原版容量而不是丢失或掉落；满时返回 {@code -1}。
     * 主背包尺寸非原版时退回原版升序扫描。
     */
    public static int firstEmptySlot(int mainSize, IntPredicate isEmpty) {
        if (mainSize != MAIN_SIZE) {
            for (int index = 0; index < mainSize; index++) {
                if (isEmpty.test(index)) {
                    return index;
                }
            }
            return -1;
        }
        for (int index : EMPTY_SLOT_ORDER) {
            if (isEmpty.test(index)) {
                return index;
            }
        }
        return -1;
    }

    /** First empty second-row slot, or {@code -1}. / 第二行第一个空槽位；没有则返回 {@code -1}。 */
    public static int firstEmptySecondRowSlot(IntPredicate isEmpty) {
        for (int index = SECOND_ROW_START; index < SECOND_ROW_START + ROW_SIZE; index++) {
            if (isEmpty.test(index)) {
                return index;
            }
        }
        return -1;
    }

    private static int[] emptySlotOrder() {
        int[] order = new int[MAIN_SIZE];
        int next = 0;
        for (int index = 0; index < ROW_SIZE; index++) {
            order[next++] = index;
        }
        for (int index = SECOND_ROW_START; index < SECOND_ROW_START + ROW_SIZE; index++) {
            order[next++] = index;
        }
        for (int index = ROW_SIZE; index < SECOND_ROW_START; index++) {
            order[next++] = index;
        }
        return order;
    }
}
