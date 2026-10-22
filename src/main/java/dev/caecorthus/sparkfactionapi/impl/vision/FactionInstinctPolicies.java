package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns instinct policy registration and priority evaluation inputs.
 * 持有直觉策略的注册与优先级评估输入。
 */
public final class FactionInstinctPolicies {
    private static final List<FactionInstinctPolicy> INSTINCT_POLICIES = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<FactionInstinctPolicy> INSTINCT_POLICIES_VIEW = Collections.unmodifiableList(INSTINCT_POLICIES);

    private FactionInstinctPolicies() {
    }

    public static List<FactionInstinctPolicy> instinctPolicies() {
        return INSTINCT_POLICIES_VIEW;
    }

    public static void register(FactionInstinctPolicy policy) {
        INSTINCT_POLICIES.add(policy);
    }

}
