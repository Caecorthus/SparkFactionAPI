package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.EffectiveFactionResolver;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns effective-faction resolver registration and ordered evaluation.
 * 持有有效阵营解析器的注册与顺序评估。
 */
public final class EffectiveFactionResolvers {
    private static final List<EffectiveFactionResolver> EFFECTIVE_RESOLVERS = new ArrayList<>();

    private EffectiveFactionResolvers() {
    }

    public static void register(EffectiveFactionResolver resolver) {
        EFFECTIVE_RESOLVERS.add(resolver);
    }

    public static Identifier resolve(PlayerEntity player, GameWorldComponent gameComponent) {
        FactionRegistryBootstrap.bootstrap();
        if (player == null || gameComponent == null) {
            return FactionIds.NONE;
        }
        return resolveFrom(FactionRoleCatalog.resolveBaseFaction(gameComponent.getRole(player)), player, gameComponent);
    }

    public static Identifier resolveFrom(
            Identifier currentFaction,
            PlayerEntity player,
            GameWorldComponent gameComponent
    ) {
        Identifier current = currentFaction;
        for (EffectiveFactionResolver resolver : EFFECTIVE_RESOLVERS) {
            Identifier resolved = resolver.resolve(player, gameComponent, current);
            if (resolved != null) {
                current = resolved;
            }
        }
        return current;
    }

    public static void clearForTests() {
        EFFECTIVE_RESOLVERS.clear();
    }
}
