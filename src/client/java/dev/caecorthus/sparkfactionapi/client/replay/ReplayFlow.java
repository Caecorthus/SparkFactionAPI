package dev.caecorthus.sparkfactionapi.client.replay;

/**
 * Left-to-right flow layout with wrapping, used by the filter tabs and the badge pills. When {@code maxRows} cuts the
 * items short, room for one overflow item ("+N") is kept at the end of the last row.
 * 自左向右、可换行的流式布局，用于筛选标签与词条小标签。{@code maxRows} 截断时，在最后一行末尾预留一个溢出项（“+N”）的位置。
 */
final class ReplayFlow {
    private ReplayFlow() {
    }

    /**
     * @param x           left offset of each shown item / 每个已显示项的左偏移
     * @param row         row of each shown item / 每个已显示项所在行
     * @param shown       how many leading items are shown / 显示的前若干项数量
     * @param overflowX   left offset of the overflow item, or -1 when everything fits / 溢出项左偏移，全部放下时为 -1
     * @param overflowRow row of the overflow item / 溢出项所在行
     * @param rows        rows used (0 for no items) / 使用的行数（无项目时为 0）
     */
    record Result(int[] x, int[] row, int shown, int overflowX, int overflowRow, int rows) {
        boolean overflowed() {
            return overflowX >= 0;
        }
    }

    /**
     * Items wider than {@code maxWidth} are clamped to it (callers ellipsize their label to match). {@code maxRows} <= 0
     * means unlimited rows.
     * 宽于 {@code maxWidth} 的项按其截断（调用方需相应省略标签）。{@code maxRows} <= 0 表示不限行数。
     */
    static Result layout(int[] widths, int maxWidth, int gap, int maxRows, int overflowWidth) {
        int count = widths.length;
        int[] xs = new int[count];
        int[] rows = new int[count];
        int limit = Math.max(1, maxWidth);
        int cursor = 0;
        int row = 0;
        int shown = 0;
        for (int i = 0; i < count; i++) {
            int width = clamped(widths[i], limit);
            if (cursor > 0 && cursor + width > limit) {
                row++;
                cursor = 0;
            }
            if (maxRows > 0 && row >= maxRows) {
                break;
            }
            xs[i] = cursor;
            rows[i] = row;
            cursor += width + gap;
            shown++;
        }
        if (shown == count) {
            return new Result(xs, rows, shown, -1, 0, count == 0 ? 0 : row + 1);
        }
        // Make room for the overflow item on the last row by dropping trailing items there.
        // 去掉最后一行末尾的项，为溢出项腾出位置。
        int lastRow = maxRows - 1;
        int overflow = Math.min(Math.max(0, overflowWidth), limit);
        while (shown > 0 && rows[shown - 1] == lastRow
                && xs[shown - 1] + clamped(widths[shown - 1], limit) + gap + overflow > limit) {
            shown--;
        }
        int overflowX = 0;
        if (shown > 0 && rows[shown - 1] == lastRow) {
            overflowX = xs[shown - 1] + clamped(widths[shown - 1], limit) + gap;
        }
        return new Result(xs, rows, shown, overflowX, lastRow, maxRows);
    }

    private static int clamped(int width, int limit) {
        return Math.min(Math.max(0, width), limit);
    }
}
