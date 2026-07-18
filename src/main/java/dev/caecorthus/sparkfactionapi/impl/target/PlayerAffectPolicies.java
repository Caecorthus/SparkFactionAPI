package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.PlayerAffectPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Owns player-affect policy registration without assigning precedence.
 * 持有玩家影响策略注册，但不赋予任何策略优先级。
 */
public final class PlayerAffectPolicies {
    private static final List<PlayerAffectPolicy> POLICIES = new ArrayList<>();

    private PlayerAffectPolicies() {
    }

    public static List<PlayerAffectPolicy> policies() {
        return Collections.unmodifiableList(new ArrayList<>(POLICIES));
    }

    public static void register(PlayerAffectPolicy policy) {
        POLICIES.add(Objects.requireNonNull(policy, "policy"));
    }
}
