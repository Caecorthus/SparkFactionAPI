package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns instinct policy registration and priority evaluation inputs.
 * 持有直觉策略的注册与优先级评估输入。
 */
public final class FactionInstinctPolicies {
    private static final List<FactionInstinctPolicy> INSTINCT_POLICIES = new ArrayList<>();

    private FactionInstinctPolicies() {
    }

    public static List<FactionInstinctPolicy> instinctPolicies() {
        return Collections.unmodifiableList(new ArrayList<>(INSTINCT_POLICIES));
    }

    public static void register(FactionInstinctPolicy policy) {
        INSTINCT_POLICIES.add(policy);
    }

}
