package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Captures victim/killer state when {@code GameFunctions.killPlayer} starts and adds it to the {@code death} record
 * that the same kill writes through {@code GameRecordManager.recordDeath}. Wathe clears psycho mode and other mods may
 * change roles between the two points, so the state must be read first. Nothing here may break a kill: every failure
 * is logged and the kill continues without the extra fields.
 * 在 {@code GameFunctions.killPlayer} 开始时捕获受害者/凶手状态，并写入同一次击杀经由
 * {@code GameRecordManager.recordDeath} 产生的 {@code death} 记录。两点之间 Wathe 会清除疯魔模式、其他模组可能改变身份，
 * 因此必须先读取。此处任何失败都不能破坏击杀：失败只记日志，击杀照常进行，只是缺少额外字段。
 */
public final class DeathRecordEnricher {
    private static final PlayerScopeStack.PerThread<DeathSnapshot> OPEN_KILLS = new PlayerScopeStack.PerThread<>();

    private DeathRecordEnricher() {
    }

    /**
     * Called before the whole {@code killPlayer} body (and before every HEAD injector); pair with {@link #endKill}
     * in a {@code finally}. Returns null when nothing was captured.
     * 在整个 {@code killPlayer} 方法体（以及所有 HEAD 注入）之前调用；须在 {@code finally} 中配对调用
     * {@link #endKill}。未捕获时返回 null。
     */
    public static PlayerScopeStack.@Nullable Frame<DeathSnapshot> beginKill(
            @Nullable ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer
    ) {
        try {
            if (victim == null || !GameRecordManager.hasActiveMatch()) {
                return null;
            }
            return OPEN_KILLS.push(victim.getUuid(), capture(victim, killer));
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to capture the kill-start record state of {}", victim.getUuid(), e);
            return null;
        }
    }

    public static void endKill(PlayerScopeStack.@Nullable Frame<DeathSnapshot> frame) {
        OPEN_KILLS.pop(frame);
    }

    /**
     * Adds the kill-start fields to the {@code death} record data before Wathe copies it into the match record.
     * Returns {@code data} itself.
     * 在 Wathe 把 {@code death} 记录数据复制进对局记录前加入击杀开始时的字段。返回 {@code data} 本身。
     */
    public static NbtCompound enrich(
            @Nullable ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            @Nullable NbtCompound data
    ) {
        if (victim == null || data == null) {
            return data;
        }
        try {
            DeathSnapshot snapshot = OPEN_KILLS.innermost(victim.getUuid());
            if (snapshot != null) {
                snapshot.writeTo(data, killer == null ? null : killer.getUuid());
            }
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to add kill-start fields to the death record of {}", victim.getUuid(), e);
        }
        return data;
    }

    private static DeathSnapshot capture(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        GameWorldComponent victimGame = GameWorldComponent.KEY.get(victim.getWorld());
        DeathSnapshot.Killer killerSnapshot = null;
        if (killer != null) {
            GameWorldComponent killerGame = GameWorldComponent.KEY.get(killer.getWorld());
            killerSnapshot = new DeathSnapshot.Killer(
                    killer.getUuid(),
                    roleId(killerGame, killer),
                    SparkFactionApi.resolveEffectiveFaction(killer, killerGame).toString(),
                    isPsycho(killer),
                    Registries.ITEM.getId(killer.getMainHandStack().getItem()).toString(),
                    killer.getWorld() == victim.getWorld() ? killer.getPos().distanceTo(victim.getPos()) : null
            );
        }
        return new DeathSnapshot(
                victim.getUuid(),
                roleId(victimGame, victim),
                SparkFactionApi.resolveEffectiveFaction(victim, victimGame).toString(),
                isPsycho(victim),
                killerSnapshot
        );
    }

    private static @Nullable String roleId(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        return DeathSnapshot.roleId(role == null ? null : role.identifier());
    }

    // Same predicate Wathe's killPlayer uses for the psycho shield. 与 Wathe killPlayer 判断疯魔护盾的条件相同。
    private static boolean isPsycho(ServerPlayerEntity player) {
        return PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }
}
