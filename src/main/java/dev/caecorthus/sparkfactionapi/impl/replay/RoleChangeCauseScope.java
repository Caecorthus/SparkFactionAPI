package dev.caecorthus.sparkfactionapi.impl.replay;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Thread-confined stack of role-change causes opened by {@code SparkReplayApi.withRoleChangeCause}.
 * 由 {@code SparkReplayApi.withRoleChangeCause} 开启的线程内身份变化原因栈。
 */
public final class RoleChangeCauseScope {
    private static final ThreadLocal<Deque<Cause>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private RoleChangeCauseScope() {
    }

    public static void run(Identifier cause, @Nullable UUID sourceUuid, Runnable action) {
        Deque<Cause> stack = STACK.get();
        stack.push(new Cause(cause, sourceUuid));
        try {
            action.run();
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                STACK.remove();
            }
        }
    }

    /** Innermost open cause on this thread, or null. 当前线程最内层的原因；没有时为 null。 */
    public static @Nullable Cause current() {
        Deque<Cause> stack = STACK.get();
        Cause cause = stack.peek();
        if (stack.isEmpty()) {
            STACK.remove();
        }
        return cause;
    }

    public record Cause(Identifier id, @Nullable UUID sourceUuid) {
    }
}
