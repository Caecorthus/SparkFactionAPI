package dev.caecorthus.sparkfactionapi.impl.blackout;

import dev.caecorthus.sparkfactionapi.api.FactionBlackoutCooldownPolicy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns blackout cooldown policy registration and ordering.
 * 持有熄灯冷却策略的注册与顺序。
 */
public final class FactionBlackoutCooldownPolicies {
    private static final List<FactionBlackoutCooldownPolicy> BLACKOUT_COOLDOWN_POLICIES = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<FactionBlackoutCooldownPolicy> BLACKOUT_COOLDOWN_POLICIES_VIEW = Collections.unmodifiableList(BLACKOUT_COOLDOWN_POLICIES);

    private FactionBlackoutCooldownPolicies() {
    }

    public static List<FactionBlackoutCooldownPolicy> blackoutCooldownPolicies() {
        return BLACKOUT_COOLDOWN_POLICIES_VIEW;
    }

    public static void register(FactionBlackoutCooldownPolicy policy) {
        BLACKOUT_COOLDOWN_POLICIES.add(policy);
    }

}
