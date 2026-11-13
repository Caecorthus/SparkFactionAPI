package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.EffectiveFactionResolver;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns effective-faction resolver registration and ordered evaluation.
 * 持有有效阵营解析器的注册与顺序评估。
 */
public final class EffectiveFactionResolvers {
    // Copy-on-write like the policy registries: read on the server and client threads on every capability lookup,
    // written only at registration. Iteration cost and order are those of the backing array.
    // 与策略注册表一样使用写时复制：每次能力查询都会在服务端与客户端线程读取，只在注册时写入。
    // 遍历开销与顺序即底层数组的开销与顺序。
    private static final List<EffectiveFactionResolver> EFFECTIVE_RESOLVERS = new CopyOnWriteArrayList<>();

    private EffectiveFactionResolvers() {
    }

    public static void register(EffectiveFactionResolver resolver) {
        EFFECTIVE_RESOLVERS.add(Objects.requireNonNull(resolver, "resolver"));
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

}
