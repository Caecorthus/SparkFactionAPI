package dev.caecorthus.sparkfactionapi.impl.roundend;

import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Owns custom faction winner collection for round-end state.
 * 持有自定义阵营结算胜者收集规则。
 */
public final class FactionWinnerCollector {
    private FactionWinnerCollector() {
    }

    public static Set<UUID> collectFactionWinners(
            ServerWorld world,
            GameWorldComponent gameComponent,
            Identifier factionId
    ) {
        return collectFactionWinners(gameComponent.getAllPlayers(), uuid -> {
            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (player != null && gameComponent.hasAnyRole(player)) {
                return FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent);
            }
            return FactionRegistryImpl.resolveBaseFaction(gameComponent.getRole(uuid));
        }, factionId);
    }

    static Set<UUID> collectFactionWinners(
            Iterable<UUID> roundPlayers,
            Function<UUID, Identifier> factionResolver,
            Identifier factionId
    ) {
        LinkedHashSet<UUID> winners = new LinkedHashSet<>();
        for (UUID uuid : roundPlayers) {
            if (factionId.equals(factionResolver.apply(uuid))) {
                winners.add(uuid);
            }
        }
        return winners;
    }
}
