package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Turns Wathe {@code GameWorldComponent#addRole} writes into {@code sparkfactionapi:role_changed} record events.
 * 将 Wathe {@code GameWorldComponent#addRole} 写入转换为 {@code sparkfactionapi:role_changed} 记录事件。
 */
public final class RoleChangeRecorder {
    // readFromNbt -> setRoles -> addRole replays saved/synced roles; depth tolerates nested reads on one thread.
    // readFromNbt -> setRoles -> addRole 会重放存档/同步的身份；按线程计数以容忍嵌套读取。
    private static final ThreadLocal<int[]> RESTORE_DEPTH = new ThreadLocal<>();

    private RoleChangeRecorder() {
    }

    public static void beginRestore() {
        int[] depth = RESTORE_DEPTH.get();
        if (depth == null) {
            depth = new int[1];
            RESTORE_DEPTH.set(depth);
        }
        depth[0]++;
    }

    public static void endRestore() {
        int[] depth = RESTORE_DEPTH.get();
        if (depth == null || --depth[0] <= 0) {
            RESTORE_DEPTH.remove();
        }
    }

    static boolean isRestoring() {
        int[] depth = RESTORE_DEPTH.get();
        return depth != null && depth[0] > 0;
    }

    /**
     * Called after an {@code addRole} overload returns; {@code before} is the role read before the write.
     * Never throws into gameplay.
     * 在 {@code addRole} 返回后调用；{@code before} 为写入前读取的身份。绝不向玩法逻辑抛出异常。
     */
    public static void afterRoleWrite(World world, GameWorldComponent game, @Nullable UUID player, @Nullable Role before) {
        if (player == null) {
            return;
        }
        try {
            record(world, game, player, before);
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record replay role change for {}", player, e);
        }
    }

    private static void record(World world, GameWorldComponent game, UUID player, @Nullable Role before) {
        boolean serverWorld = !world.isClient();
        if (!RoleChangeRecordRules.isRecordingContext(
                serverWorld,
                isRestoring(),
                serverWorld && GameRecordManager.hasActiveMatch(),
                game.isRunning()
        )) {
            return;
        }
        Identifier from = roleId(before);
        Identifier to = roleId(game.getRole(player));
        if (!RoleChangeRecordRules.isRoleChange(from, to) || !(world instanceof ServerWorld server)) {
            return;
        }

        GameRecordManager.EventBuilder event = GameRecordManager.event(SparkReplayApi.ROLE_CHANGED_EVENT_TYPE).world(server);
        ServerPlayerEntity online = server.getServer().getPlayerManager().getPlayer(player);
        if (online != null) {
            event.actor(online);
        }
        event.putUuid(ReplayRoleTimeline.KEY_PLAYER, player);
        Identifier recordedFrom = RoleChangeRecordRules.recordedFrom(from);
        if (recordedFrom != null) {
            event.put(ReplayRoleTimeline.KEY_FROM, recordedFrom.toString());
        }
        event.put(ReplayRoleTimeline.KEY_TO, to.toString());
        RoleChangeCauseScope.Cause cause = RoleChangeRecordRules.resolveCause(RoleChangeCauseScope.current(), from, to);
        if (cause != null) {
            event.put(ReplayRoleTimeline.KEY_CAUSE, cause.id().toString());
            if (cause.sourceUuid() != null) {
                event.putUuid(ReplayRoleTimeline.KEY_SOURCE, cause.sourceUuid());
            }
        }
        event.record();
    }

    private static @Nullable Identifier roleId(@Nullable Role role) {
        return role == null ? null : role.identifier();
    }
}
