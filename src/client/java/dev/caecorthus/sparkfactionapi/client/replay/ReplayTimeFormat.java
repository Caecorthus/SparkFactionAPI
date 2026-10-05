package dev.caecorthus.sparkfactionapi.client.replay;

import net.minecraft.text.Text;

/**
 * Match-time and "ended ago" formatting for the replay screen.
 * 回放界面的对局时间与“多久前结束”格式化。
 */
final class ReplayTimeFormat {
    static final String KEY_SECONDS = "screen.sparkfactionapi.replay.time.seconds";
    static final String KEY_MINUTES = "screen.sparkfactionapi.replay.time.minutes";
    static final String KEY_HOURS = "screen.sparkfactionapi.replay.time.hours";

    private ReplayTimeFormat() {
    }

    /**
     * "MM:SS"; minutes are not capped at 59 (a 125-minute match reads "125:07"). Negative input reads "00:00".
     * “MM:SS”；分钟不在 59 处进位为小时（125 分钟显示为“125:07”）；负数显示“00:00”。
     */
    static String clock(int seconds) {
        int safe = Math.max(0, seconds);
        int minutes = safe / 60;
        int rest = safe % 60;
        StringBuilder out = new StringBuilder(6);
        if (minutes < 10) {
            out.append('0');
        }
        out.append(minutes).append(':');
        if (rest < 10) {
            out.append('0');
        }
        return out.append(rest).toString();
    }

    /** Largest whole unit of an elapsed time. 经过时间的最大整单位。 */
    record Ago(String key, int value) {
    }

    /**
     * Below a minute counts seconds, below an hour whole minutes, else whole hours (floored).
     * 不足一分钟按秒、不足一小时按整分钟、否则按整小时（向下取整）。
     */
    static Ago ago(int seconds) {
        int safe = Math.max(0, seconds);
        if (safe < 60) {
            return new Ago(KEY_SECONDS, safe);
        }
        if (safe < 3600) {
            return new Ago(KEY_MINUTES, safe / 60);
        }
        return new Ago(KEY_HOURS, safe / 3600);
    }

    static Text agoText(int seconds) {
        Ago ago = ago(seconds);
        return Text.translatable(ago.key(), ago.value());
    }
}
