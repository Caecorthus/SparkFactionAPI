package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns target eligibility policy registration and ordering.
 * 持有目标资格策略的注册与顺序。
 */
public final class FactionTargetPolicies {
    private static final List<FactionTargetEligibility> TARGET_ELIGIBILITY = new ArrayList<>();

    private FactionTargetPolicies() {
    }

    public static List<FactionTargetEligibility> targetEligibility() {
        return Collections.unmodifiableList(new ArrayList<>(TARGET_ELIGIBILITY));
    }

    public static void register(FactionTargetEligibility eligibility) {
        TARGET_ELIGIBILITY.add(eligibility);
    }

}
