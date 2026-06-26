package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FactionTargetEligibility {
    @Nullable
    Boolean canTarget(PlayerEntity viewer, PlayerEntity target, Identifier targetTag, GameWorldComponent gameComponent);
}
