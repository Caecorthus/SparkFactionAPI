package dev.caecorthus.sparkfactionapi.impl.gun;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns gun punishment policy registration and ordering.
 * 持有枪罚策略的注册与顺序。
 */
public final class FactionGunPunishmentPolicies {
    private static final List<FactionGunPunishmentPolicy> GUN_PUNISHMENT_POLICIES = new ArrayList<>();

    private FactionGunPunishmentPolicies() {
    }

    public static List<FactionGunPunishmentPolicy> gunPunishmentPolicies() {
        return Collections.unmodifiableList(new ArrayList<>(GUN_PUNISHMENT_POLICIES));
    }

    public static void register(FactionGunPunishmentPolicy policy) {
        GUN_PUNISHMENT_POLICIES.add(policy);
    }

}
