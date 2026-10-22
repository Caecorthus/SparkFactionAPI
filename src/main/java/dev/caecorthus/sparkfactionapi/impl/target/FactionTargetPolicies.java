package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns target eligibility policy registration and ordering.
 * 持有目标资格策略的注册与顺序。
 */
public final class FactionTargetPolicies {
    private static final List<FactionTargetEligibility> TARGET_ELIGIBILITY = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<FactionTargetEligibility> TARGET_ELIGIBILITY_VIEW = Collections.unmodifiableList(TARGET_ELIGIBILITY);

    private FactionTargetPolicies() {
    }

    public static List<FactionTargetEligibility> targetEligibility() {
        return TARGET_ELIGIBILITY_VIEW;
    }

    public static void register(FactionTargetEligibility eligibility) {
        TARGET_ELIGIBILITY.add(eligibility);
    }

}
