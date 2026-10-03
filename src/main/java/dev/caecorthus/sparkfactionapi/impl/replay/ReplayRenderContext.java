package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-confined state of the replay currently being generated. Wathe builds and sends the replay synchronously on
 * the server thread, so formatter callbacks that run inside that call can find the active session and event here.
 * 当前正在生成的回放的线程内状态。Wathe 在服务端线程同步生成并发送回放，因此在其调用链内执行的格式化器可在此找到
 * 活动会话与当前事件。
 */
public final class ReplayRenderContext {
    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private ReplayRenderContext() {
    }

    public static void install(ReplaySession session) {
        STATE.set(new State(session));
    }

    public static void clear() {
        STATE.remove();
    }

    public static @Nullable ReplaySession session() {
        State state = STATE.get();
        return state == null ? null : state.session;
    }

    public static @Nullable GameRecordEvent currentEvent() {
        State state = STATE.get();
        return state == null ? null : state.currentEvent;
    }

    /**
     * Sets the event being formatted and returns the previous one so callers can restore it in {@code finally}.
     * No-op outside a session.
     * 设置正在格式化的事件并返回之前的事件，供调用方在 {@code finally} 中恢复；会话之外不做任何事。
     */
    public static @Nullable GameRecordEvent swapCurrentEvent(@Nullable GameRecordEvent event) {
        State state = STATE.get();
        if (state == null) {
            return null;
        }
        GameRecordEvent previous = state.currentEvent;
        state.currentEvent = event;
        return previous;
    }

    private static final class State {
        private final ReplaySession session;
        private @Nullable GameRecordEvent currentEvent;

        private State(ReplaySession session) {
            this.session = session;
        }
    }
}
