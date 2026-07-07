package dev.caecorthus.sparkfactionapi.impl.economy;

import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns economy policy registration and ordering.
 * 持有经济策略的注册与顺序。
 */
public final class FactionEconomyPolicies {
    private static final List<FactionEconomyPolicy> ECONOMY_POLICIES = new ArrayList<>();

    private FactionEconomyPolicies() {
    }

    public static List<FactionEconomyPolicy> economyPolicies() {
        return Collections.unmodifiableList(new ArrayList<>(ECONOMY_POLICIES));
    }

    public static void register(FactionEconomyPolicy policy) {
        ECONOMY_POLICIES.add(policy);
    }

}
