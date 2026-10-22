package dev.caecorthus.sparkfactionapi.impl.gun;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns gun punishment policy registration and ordering.
 * 持有枪罚策略的注册与顺序。
 */
public final class FactionGunPunishmentPolicies {
    private static final List<FactionGunPunishmentPolicy> GUN_PUNISHMENT_POLICIES = new CopyOnWriteArrayList<>();
    // Read-only live view: callers iterate a copy-on-write snapshot without a per-call copy.
    // 只读实时视图：遍历时使用写时复制快照，避免每次读取都复制列表。
    private static final List<FactionGunPunishmentPolicy> GUN_PUNISHMENT_POLICIES_VIEW = Collections.unmodifiableList(GUN_PUNISHMENT_POLICIES);

    private FactionGunPunishmentPolicies() {
    }

    public static List<FactionGunPunishmentPolicy> gunPunishmentPolicies() {
        return GUN_PUNISHMENT_POLICIES_VIEW;
    }

    public static void register(FactionGunPunishmentPolicy policy) {
        GUN_PUNISHMENT_POLICIES.add(policy);
    }

}
