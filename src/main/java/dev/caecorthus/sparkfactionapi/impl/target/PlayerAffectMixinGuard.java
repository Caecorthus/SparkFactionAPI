package dev.caecorthus.sparkfactionapi.impl.target;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Shared bridge used by mixin adapters without placing a directly loaded helper in the Mixin-owned package.
 * 为 mixin 适配器提供共享桥接，同时避免把直接加载的工具类放进 Mixin 所有的包。
 */
public final class PlayerAffectMixinGuard {
    private PlayerAffectMixinGuard() {
    }

    public static boolean allows(PlayerEntity actor, PlayerEntity target, Identifier actionId) {
        if (actor == null || target == null || actor == target) {
            return true;
        }
        return SparkFactionApi.canAffectPlayer(
                actor,
                target,
                actionId,
                GameWorldComponent.KEY.get(actor.getWorld())
        );
    }

    @Nullable
    public static ServerPlayerEntity onlineTarget(ServerPlayerEntity actor, UUID targetUuid) {
        return targetUuid == null || actor.getServer() == null
                ? null
                : actor.getServer().getPlayerManager().getPlayer(targetUuid);
    }

    @Nullable
    public static ServerPlayerEntity entityTarget(ServerPlayerEntity actor, Integer entityId) {
        if (entityId == null) {
            return null;
        }
        return actor.getServerWorld().getEntityById(entityId) instanceof ServerPlayerEntity target
                ? target
                : null;
    }

    @Nullable
    public static UUID uuidAccessor(Object payload, String accessor) {
        Object value = invokeAccessor(payload, accessor);
        return value instanceof UUID uuid ? uuid : null;
    }

    @Nullable
    public static Integer intAccessor(Object payload, String accessor) {
        Object value = invokeAccessor(payload, accessor);
        return value instanceof Integer integer ? integer : null;
    }

    @Nullable
    private static Object invokeAccessor(Object payload, String accessor) {
        if (payload == null) {
            return null;
        }
        try {
            Method method = payload.getClass().getMethod(accessor);
            return method.invoke(payload);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
