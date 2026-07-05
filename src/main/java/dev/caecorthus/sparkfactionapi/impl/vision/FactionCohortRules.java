package dev.caecorthus.sparkfactionapi.impl.vision;

import dev.caecorthus.sparkfactionapi.impl.capability.FactionCapabilityLookup;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class FactionCohortRules {
    private FactionCohortRules() {
    }

    public static boolean sharesCohort(Role viewerRole, Role targetRole) {
        Identifier viewerFaction = FactionCapabilityLookup.baseFaction(viewerRole);
        Identifier targetFaction = FactionCapabilityLookup.baseFaction(targetRole);
        return viewerFaction.equals(targetFaction)
                && FactionCapabilityLookup.capabilities(viewerRole).sharesCohort()
                && FactionCapabilityLookup.capabilities(targetRole).sharesCohort();
    }

    public static boolean sharesCohort(
            PlayerEntity viewer,
            PlayerEntity target,
            GameWorldComponent gameComponent
    ) {
        Identifier viewerFaction = FactionCapabilityLookup.effectiveFaction(viewer, gameComponent);
        Identifier targetFaction = FactionCapabilityLookup.effectiveFaction(target, gameComponent);
        return viewerFaction.equals(targetFaction)
                && FactionCapabilityLookup.capabilities(viewer, gameComponent).sharesCohort()
                && FactionCapabilityLookup.capabilities(target, gameComponent).sharesCohort();
    }
}
