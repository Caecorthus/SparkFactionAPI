package dev.caecorthus.sparkfactionapi.impl.record;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-thread bookkeeping of open, per-player call scopes: kill-start snapshots for the {@code death} record (one frame
 * per {@code killPlayer} call) and open {@code ItemStack#finishUsing} calls for {@code sparkfactionapi:consume}. Every
 * scope pops its exact frame when it returns, so a cancelled or early-returning call leaves nothing behind. A call
 * nested in another (a kill that triggers a kill) pushes its own frame; lookups pick the innermost frame of that player.
 * 按线程记录进行中的、按玩家划分的调用作用域：{@code death} 记录的击杀开始快照（每次 {@code killPlayer} 调用一帧），以及
 * {@code sparkfactionapi:consume} 的进行中 {@code ItemStack#finishUsing} 调用。每个作用域返回时都弹出自己的那一帧，因此
 * 被取消或提前返回的调用不会残留。嵌套调用（一次击杀引发另一次击杀）会压入自己的帧；查找时取该玩家最内层的帧。
 */
public final class PlayerScopeStack<T> {
    /** One open scope. Compared by identity. 一个进行中的作用域，按引用比较。 */
    public static final class Frame<T> {
        private final UUID player;
        private final T value;

        private Frame(UUID player, T value) {
            this.player = player;
            this.value = value;
        }

        public UUID player() {
            return player;
        }

        public T value() {
            return value;
        }
    }

    /**
     * One stack per thread, dropped from the thread once it is empty again.
     * 每个线程一个栈，再次为空时从线程上移除。
     */
    public static final class PerThread<T> {
        private final ThreadLocal<PlayerScopeStack<T>> stacks = new ThreadLocal<>();

        public Frame<T> push(UUID player, T value) {
            PlayerScopeStack<T> stack = stacks.get();
            if (stack == null) {
                stack = new PlayerScopeStack<>();
                stacks.set(stack);
            }
            return stack.push(player, value);
        }

        public void pop(@Nullable Frame<T> frame) {
            PlayerScopeStack<T> stack = stacks.get();
            if (stack == null) {
                return;
            }
            stack.pop(frame);
            if (stack.isEmpty()) {
                stacks.remove();
            }
        }

        public @Nullable T innermost(@Nullable UUID player) {
            PlayerScopeStack<T> stack = stacks.get();
            return stack == null ? null : stack.innermost(player);
        }

        public boolean isEmpty() {
            PlayerScopeStack<T> stack = stacks.get();
            return stack == null || stack.isEmpty();
        }
    }

    // Head = innermost (most recent) scope. 队首为最内层（最近）的作用域。
    private final Deque<Frame<T>> frames = new ArrayDeque<>();

    public Frame<T> push(UUID player, T value) {
        Frame<T> frame = new Frame<>(Objects.requireNonNull(player, "player"), Objects.requireNonNull(value, "value"));
        frames.push(frame);
        return frame;
    }

    /**
     * Removes exactly {@code frame}, wherever it is, so an unbalanced inner frame cannot make an outer scope pop the
     * wrong one. Unknown or null frames are ignored.
     * 精确移除 {@code frame}（无论位置），避免内层未平衡的帧导致外层作用域弹出错误的帧。未知或 null 帧会被忽略。
     */
    public void pop(@Nullable Frame<T> frame) {
        if (frame == null) {
            return;
        }
        frames.removeIf(open -> open == frame);
    }

    /** Value of the innermost open scope of {@code player}, or null. 返回 {@code player} 最内层进行中作用域的值，或 null。 */
    public @Nullable T innermost(@Nullable UUID player) {
        if (player == null) {
            return null;
        }
        for (Frame<T> frame : frames) {
            if (frame.player.equals(player)) {
                return frame.value;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public int size() {
        return frames.size();
    }
}
