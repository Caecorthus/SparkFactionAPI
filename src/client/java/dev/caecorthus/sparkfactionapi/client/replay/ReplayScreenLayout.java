package dev.caecorthus.sparkfactionapi.client.replay;

/**
 * Pure geometry of the replay screen for one scaled window size. Every region lies inside {@code panel}; the roster
 * sits left of the timeline, or above it as a shorter strip when the window is narrower than
 * {@link #STACK_BELOW_WIDTH}. Compact vertical metrics apply below {@link #COMPACT_BELOW_HEIGHT}.
 * 回放界面在某一缩放窗口尺寸下的纯几何。所有区域都位于 {@code panel} 内；名单在时间线左侧，窗口宽度小于
 * {@link #STACK_BELOW_WIDTH} 时改为位于时间线上方的较矮条带。高度小于 {@link #COMPACT_BELOW_HEIGHT} 时使用紧凑的纵向尺寸。
 *
 * @param panel       whole framed panel / 整个带边框的面板
 * @param header      title + subtitle area (left part) and chip (right part) / 标题与副标题区域
 * @param close       close button / 关闭按钮
 * @param roster      roster column or strip (title + list) / 名单栏或条带（标题 + 列表）
 * @param rosterTitle "Players (N)" row / “玩家（N）”行
 * @param rosterList  scrollable roster viewport / 可滚动的名单视口
 * @param timeline    timeline column (tabs .. list) / 时间线栏（标签至列表）
 * @param tabs        filter tab rows / 筛选标签行
 * @param search      search well / 搜索框凹槽
 * @param count       "shown / total" text box, empty when it does not fit / “显示 / 总数”文本框，放不下时为空
 * @param chip        player filter chip row, empty when no player is selected / 玩家筛选标签行，未选玩家时为空
 * @param list        scrollable timeline viewport / 可滚动的时间线视口
 * @param bar         bottom timeline bar / 底部时间轴
 */
record ReplayScreenLayout(
        boolean stacked,
        boolean compact,
        ReplayRect panel,
        ReplayRect header,
        ReplayRect close,
        ReplayRect roster,
        ReplayRect rosterTitle,
        ReplayRect rosterList,
        ReplayRect timeline,
        ReplayRect tabs,
        ReplayRect search,
        ReplayRect count,
        ReplayRect chip,
        ReplayRect list,
        ReplayRect bar
) {
    static final int STACK_BELOW_WIDTH = 420;
    static final int COMPACT_BELOW_HEIGHT = 280;
    static final int PANEL_MAX_WIDTH = 1000;
    static final int PANEL_MAX_HEIGHT = 620;
    /** Frame (3 px) plus padding between the panel edge and its content. 面板边缘到内容的边框（3 像素）与留白。 */
    static final int FRAME = 6;
    static final int CLOSE_SIZE = 13;
    static final int ROSTER_MIN = 150;
    static final int ROSTER_MAX = 240;
    static final double ROSTER_SHARE = 0.34;
    static final int COLUMN_GAP = 9;
    static final int STACK_GAP = 6;
    static final int ROSTER_TITLE_HEIGHT = 12;
    static final int TAB_ROW_HEIGHT = 12;
    static final int TAB_ROW_GAP = 2;
    static final int SEARCH_HEIGHT = 14;
    static final int CHIP_HEIGHT = 11;
    static final int ROW_GAP = 3;
    static final int MIN_SEARCH_WIDTH = 60;
    static final int COUNT_GAP = 6;

    /**
     * @param width        scaled window width / 缩放后窗口宽度
     * @param height       scaled window height / 缩放后窗口高度
     * @param tabRows      rows the filter tabs need at {@link #timeline()} width (at least 1) / 筛选标签在时间线宽度下所需行数
     * @param playerChip   whether the player filter chip row is shown / 是否显示玩家筛选标签行
     * @param countReserve width reserved for the "shown / total" text / 为“显示 / 总数”文本预留的宽度
     */
    static ReplayScreenLayout compute(int width, int height, int tabRows, boolean playerChip, int countReserve) {
        int w = Math.max(0, width);
        int h = Math.max(0, height);
        boolean stacked = w < STACK_BELOW_WIDTH;
        boolean compact = h < COMPACT_BELOW_HEIGHT;
        int marginX = w < STACK_BELOW_WIDTH ? 6 : w < 560 ? 12 : 20;
        int marginY = compact ? 6 : h < 400 ? 12 : 18;
        int panelW = Math.min(PANEL_MAX_WIDTH, Math.max(0, w - 2 * marginX));
        int panelH = Math.min(PANEL_MAX_HEIGHT, Math.max(0, h - 2 * marginY));
        ReplayRect panel = new ReplayRect((w - panelW) / 2, (h - panelH) / 2, panelW, panelH);
        ReplayRect inner = panel.inset(FRAME);

        int headerH = Math.min(compact ? 22 : 26, inner.height());
        int headerGap = compact ? 3 : 5;
        int barH = compact ? 20 : 24;
        int barGap = compact ? 4 : 5;
        ReplayRect header = new ReplayRect(inner.x(), inner.y(), inner.width(), headerH);
        int closeSize = Math.min(CLOSE_SIZE, Math.min(header.width(), header.height()));
        ReplayRect close = new ReplayRect(header.right() - closeSize, header.y(), closeSize, closeSize);

        int bodyTop = Math.min(inner.bottom(), header.bottom() + headerGap);
        int barTop = Math.max(bodyTop, inner.bottom() - barH);
        ReplayRect bar = new ReplayRect(inner.x(), barTop, inner.width(), inner.bottom() - barTop);
        int bodyBottom = Math.max(bodyTop, bar.y() - barGap);
        int bodyH = bodyBottom - bodyTop;

        ReplayRect roster;
        ReplayRect timeline;
        if (stacked) {
            int rosterH = clamp((int) Math.round(bodyH * 0.36), Math.min(46, bodyH / 2), 120);
            rosterH = Math.min(rosterH, bodyH);
            roster = new ReplayRect(inner.x(), bodyTop, inner.width(), rosterH);
            int timelineTop = Math.min(bodyBottom, roster.bottom() + STACK_GAP);
            timeline = new ReplayRect(inner.x(), timelineTop, inner.width(), bodyBottom - timelineTop);
        } else {
            int rosterW = clamp((int) Math.round(inner.width() * ROSTER_SHARE), ROSTER_MIN, ROSTER_MAX);
            rosterW = Math.min(rosterW, Math.max(0, inner.width() - COLUMN_GAP));
            roster = new ReplayRect(inner.x(), bodyTop, rosterW, bodyH);
            int timelineX = roster.right() + COLUMN_GAP;
            timeline = new ReplayRect(timelineX, bodyTop, inner.right() - timelineX, bodyH);
        }

        int titleH = Math.min(ROSTER_TITLE_HEIGHT, roster.height());
        ReplayRect rosterTitle = new ReplayRect(roster.x(), roster.y(), roster.width(), titleH);
        int rosterListTop = Math.min(roster.bottom(), rosterTitle.bottom() + 1);
        ReplayRect rosterList = new ReplayRect(roster.x(), rosterListTop, roster.width(), roster.bottom() - rosterListTop);

        Stack column = new Stack(timeline);
        int rows = Math.max(1, tabRows);
        ReplayRect tabs = column.take(rows * TAB_ROW_HEIGHT + (rows - 1) * TAB_ROW_GAP);
        ReplayRect searchRow = column.take(SEARCH_HEIGHT);
        ReplayRect chip = playerChip ? column.take(CHIP_HEIGHT) : new ReplayRect(timeline.x(), searchRow.bottom(), 0, 0);
        ReplayRect list = column.rest();

        int reserve = Math.max(0, countReserve);
        boolean countFits = searchRow.width() - reserve - COUNT_GAP >= MIN_SEARCH_WIDTH;
        ReplayRect search = countFits
                ? new ReplayRect(searchRow.x(), searchRow.y(), searchRow.width() - reserve - COUNT_GAP, searchRow.height())
                : searchRow;
        ReplayRect count = countFits
                ? new ReplayRect(search.right() + COUNT_GAP, searchRow.y(), searchRow.right() - search.right() - COUNT_GAP,
                searchRow.height())
                : new ReplayRect(searchRow.right(), searchRow.y(), 0, 0);

        return new ReplayScreenLayout(stacked, compact, panel, header, close, roster, rosterTitle, rosterList, timeline,
                tabs, search, count, chip, list, bar);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Takes rows from the top of a column, separated by {@link #ROW_GAP}, never past its bottom. 自上而下切分列。 */
    private static final class Stack {
        private final ReplayRect column;
        private int top;

        Stack(ReplayRect column) {
            this.column = column;
            this.top = column.y();
        }

        ReplayRect take(int height) {
            int y = top;
            int h = Math.max(0, Math.min(height, column.bottom() - y));
            top = Math.min(column.bottom(), y + h + ROW_GAP);
            return new ReplayRect(column.x(), y, column.width(), h);
        }

        ReplayRect rest() {
            return new ReplayRect(column.x(), top, column.width(), column.bottom() - top);
        }
    }
}
