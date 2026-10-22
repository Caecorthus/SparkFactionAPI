package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.PlayerAffectPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns player-affect policy registration without assigning precedence.
 * 持有玩家影响策略注册，但不赋予任何策略优先级。
 */
public final class PlayerAffectPolicies {
    private static final List<PlayerAffectPolicy> POLICIES = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<PlayerAffectPolicy> POLICIES_VIEW = Collections.unmodifiableList(POLICIES);

    private PlayerAffectPolicies() {
    }

    public static List<PlayerAffectPolicy> policies() {
        return POLICIES_VIEW;
    }

    public static void register(PlayerAffectPolicy policy) {
        POLICIES.add(Objects.requireNonNull(policy, "policy"));
    }
}
