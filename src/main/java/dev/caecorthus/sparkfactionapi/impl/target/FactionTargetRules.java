package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;
import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves custom-faction target tags and target eligibility policies.
 * 解析自定义阵营目标标签与目标资格策略。
 */
public final class FactionTargetRules {
    private FactionTargetRules() {
    }

    public static boolean canTarget(Role viewerRole, Role targetRole, Identifier targetTag) {
        return targetTag != null && FactionCapabilityLookup.capabilities(targetRole).targetTags().contains(targetTag);
    }

    public static boolean canTarget(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        if (viewer == null || target == null || targetTag == null || gameComponent == null) {
            return false;
        }
        Boolean policyResult = firstTargetEligibilityResult(viewer, target, targetTag, gameComponent);
        return policyResult == null
                ? FactionCapabilityLookup.capabilities(target, gameComponent).targetTags().contains(targetTag)
                : policyResult;
    }

    private static @Nullable Boolean firstTargetEligibilityResult(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        for (FactionTargetEligibility eligibility : FactionTargetPolicies.targetEligibility()) {
            Boolean result = eligibility.canTarget(viewer, target, targetTag, gameComponent);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
