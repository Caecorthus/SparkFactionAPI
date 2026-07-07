package dev.caecorthus.sparkfactionapi.impl.blackout;

import dev.caecorthus.sparkfactionapi.api.FactionBlackoutCooldownPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns blackout cooldown policy registration and ordering.
 * 持有熄灯冷却策略的注册与顺序。
 */
public final class FactionBlackoutCooldownPolicies {
    private static final List<FactionBlackoutCooldownPolicy> BLACKOUT_COOLDOWN_POLICIES = new ArrayList<>();

    private FactionBlackoutCooldownPolicies() {
    }

    public static List<FactionBlackoutCooldownPolicy> blackoutCooldownPolicies() {
        return Collections.unmodifiableList(new ArrayList<>(BLACKOUT_COOLDOWN_POLICIES));
    }

    public static void register(FactionBlackoutCooldownPolicy policy) {
        BLACKOUT_COOLDOWN_POLICIES.add(policy);
    }

}
