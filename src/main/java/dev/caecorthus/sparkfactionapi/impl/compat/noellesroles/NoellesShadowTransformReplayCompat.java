package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.impl.replay.RoleChangeRecordRules;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayEventFormatter;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * NoellesRoles records its own {@code shadow_transform} line right after the Shadow Jester partner becomes Jester.
 * Once SparkFactionAPI records the same change as {@code role_changed}, that Noelles line is suppressed for the
 * same player and tick. Wrapped at server start because Noelles registers its formatter during its own init.
 * NoellesRoles 会在影子小丑搭档化身小丑后立即记录自己的 {@code shadow_transform} 行。SparkFactionAPI 已将同一变化记为
 * {@code role_changed} 时，同一玩家同一刻的 Noelles 行被抑制。因 Noelles 在自身初始化时注册格式化器，故在服务器启动时包裹。
 */
public final class NoellesShadowTransformReplayCompat {
    private static final String MOD_ID = "noellesroles";
    private static final String SHADOW_TRANSFORM_EVENT_TYPE = "shadow_transform";
    // Wathe's GameRecordManager#addEvent stores the actor UUID under this key.
    // Wathe 的 GameRecordManager#addEvent 以此键存储 actor UUID。
    private static final String ACTOR_KEY = "actor";
    private static boolean registered;

    private NoellesShadowTransformReplayCompat() {
    }

    public static synchronized void register() {
        if (registered || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> install());
    }

    private static synchronized void install() {
        ReplayEventFormatter current = ReplayRegistry.getFormatter(SHADOW_TRANSFORM_EVENT_TYPE);
        if (current == null || current instanceof DedupingFormatter) {
            return;
        }
        ReplayRegistry.registerFormatter(SHADOW_TRANSFORM_EVENT_TYPE, new DedupingFormatter(current));
    }

    private record DedupingFormatter(ReplayEventFormatter delegate) implements ReplayEventFormatter {
        @Override
        public @Nullable Text format(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
            if (isSupersededByRoleChange(event, match)) {
                return null;
            }
            return delegate.format(event, match, world);
        }

        private static boolean isSupersededByRoleChange(GameRecordEvent event, GameRecordManager.MatchRecord match) {
            try {
                NbtCompound data = event.data();
                return data.containsUuid(ACTOR_KEY) && RoleChangeRecordRules.containsRoleChange(
                        match.getEvents(),
                        data.getUuid(ACTOR_KEY),
                        RoleChangeRecordRules.NOELLES_JESTER,
                        event.worldTick()
                );
            } catch (RuntimeException e) {
                SparkFactionApiMod.LOGGER.warn("Failed to dedupe NoellesRoles shadow_transform replay line", e);
                return false;
            }
        }
    }
}
