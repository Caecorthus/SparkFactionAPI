package dev.caecorthus.sparkfactionapi.impl.mapreset;

import java.util.List;

/**
 * Pure rules for the lobby map reset: progress display, the per-tick budget, the auto-start hold and which
 * administrator commands are refused while a reset runs.
 * 大厅地图重置的纯规则：进度显示、每 tick 预算、自动开局的等待判断，以及重置期间拒绝哪些管理员命令。
 */
public final class LobbyMapResetRules {
    /** Synced progress value meaning "no reset to show". / 同步进度值，表示"没有要显示的重置"。 */
    public static final int INACTIVE = -1;
    /** Synced progress value shown briefly after a reset completed. / 重置完成后短暂显示的同步进度值。 */
    public static final int COMPLETE = 100;
    /** Copy passes over the template: snapshot, barrier, place, neighbor updates. / 模板复制的遍数：快照、屏障、放置、邻居更新。 */
    public static final int PASSES = 4;

    /** Server-thread time the copy may use per tick. / 每 tick 复制可占用的服务端线程时间。 */
    public static final long TICK_BUDGET_NANOS = 8_000_000L;
    /** Blocks between two clock reads inside one tick. / 同一 tick 内两次读取时钟之间处理的方块数。 */
    public static final int CLOCK_CHECK_INTERVAL = 32;
    /** Give up when the template or paste chunks are still not loaded after this long. / 模板或粘贴区块这么久仍未加载时放弃。 */
    public static final int CHUNK_LOAD_TIMEOUT_TICKS = 60 * 20;
    /** How long "complete" stays on screen. / "完成"在屏幕上停留的时长。 */
    public static final int COMPLETE_DISPLAY_TICKS = 3 * 20;
    /** Keep the train chunks loaded this long after the round in that world started. / 该世界开局后继续保持车厢区块加载的时长。 */
    public static final int TICKET_HOLD_AFTER_START_TICKS = 10 * 20;
    /** Release the train chunks when no round started this long after the reset. / 重置后这么久仍未开局时释放车厢区块。 */
    public static final int TICKET_IDLE_RELEASE_TICKS = 30 * 60 * 20;

    static final String MAP_VOTE_LITERAL = "wathe:mapvote";
    static final String MAP_VARIABLES_LITERAL = "wathe:mapVariables";
    static final String SET_LITERAL = "set";

    private LobbyMapResetRules() {
    }

    /** A value in [0, 99] means a reset is running. / 取值在 [0, 99] 表示重置正在进行。 */
    public static boolean isResetting(int progress) {
        return progress >= 0 && progress < COMPLETE;
    }

    /**
     * Progress while copying, capped at 99 so 100 only ever means "done". Each pass covers every template block once.
     * 复制中的进度，上限为 99，保证 100 只表示"完成"。每一遍都会处理模板中的每个方块一次。
     */
    public static int percent(int pass, long doneInPass, long blocksPerPass) {
        if (blocksPerPass <= 0 || pass < 0) {
            return 0;
        }
        if (pass >= PASSES) {
            return COMPLETE - 1;
        }
        long done = pass * blocksPerPass + Math.min(Math.max(doneInPass, 0), blocksPerPass);
        long total = PASSES * blocksPerPass;
        return (int) Math.min(COMPLETE - 1, done * COMPLETE / total);
    }

    /**
     * Wathe's auto start fires when its countdown is at zero. While a reset runs, the countdown waits at zero instead
     * of firing and being refused, which would restart it from the full start time. It still restarts as usual when
     * the ready area empties.
     * Wathe 的自动开局在倒计时为零时触发。重置期间倒计时停在零，而不是触发后被拒绝（那样会从完整时长重新开始）。
     * 准备区人数不足时仍照常重新计时。
     */
    public static boolean holdsAutoStart(boolean resetting, int startTime, int time, int readyPlayers, int minPlayers) {
        return resetting && startTime > 0 && time <= 0 && readyPlayers >= minPlayers;
    }

    /**
     * The literal chain of a parsed command, including literals after {@code execute ... run}. Map voting would move
     * everyone to another map, and map variables would change the template or areas mid-copy, so both are refused.
     * {@code /wathe:mapVariables help} stays available.
     * 已解析命令的字面量链，包含 {@code execute ... run} 之后的部分。地图投票会把所有人传送到另一张地图，
     * 地图变量会在复制中途改动模板或区域，因此两者都被拒绝；{@code /wathe:mapVariables help} 仍可使用。
     */
    public static boolean blocksCommand(List<String> literals) {
        for (int i = 0; i < literals.size(); i++) {
            String literal = literals.get(i);
            if (MAP_VOTE_LITERAL.equals(literal)) {
                return true;
            }
            if (MAP_VARIABLES_LITERAL.equals(literal)
                    && i + 1 < literals.size()
                    && SET_LITERAL.equals(literals.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    /** A map-variables write outside a reset makes a finished reset stale. / 重置之外写入地图变量会让已完成的重置失效。 */
    public static boolean editsMapVariables(List<String> literals) {
        for (int i = 0; i + 1 < literals.size(); i++) {
            if (MAP_VARIABLES_LITERAL.equals(literals.get(i)) && SET_LITERAL.equals(literals.get(i + 1))) {
                return true;
            }
        }
        return false;
    }
}
