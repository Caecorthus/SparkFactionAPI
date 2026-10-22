package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns economy policy registration and ordering.
 * 持有经济策略的注册与顺序。
 */
public final class FactionEconomyPolicies {
    private static final List<FactionEconomyPolicy> ECONOMY_POLICIES = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<FactionEconomyPolicy> ECONOMY_POLICIES_VIEW = Collections.unmodifiableList(ECONOMY_POLICIES);

    private FactionEconomyPolicies() {
    }

    public static List<FactionEconomyPolicy> economyPolicies() {
        return ECONOMY_POLICIES_VIEW;
    }

    public static void register(FactionEconomyPolicy policy) {
        ECONOMY_POLICIES.add(policy);
    }

}
