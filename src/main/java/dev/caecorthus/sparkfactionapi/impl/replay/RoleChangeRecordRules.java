package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.doctor4t.wathe.record.GameRecordEvent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure decisions for recording mid-round role changes into the Wathe match record.
 * 将对局内身份变化写入 Wathe 对局记录时使用的纯判定规则。
 */
public final class RoleChangeRecordRules {
    /**
     * Wathe's placeholder role ({@code WatheRoles.NO_ROLE}); treated as "no role" on both sides of a change.
     * Wathe 的占位身份（{@code WatheRoles.NO_ROLE}）；在变化两侧都视为“无身份”。
     */
    public static final Identifier WATHE_NO_ROLE = Identifier.of("wathe", "no_role");
    public static final Identifier NOELLES_SHADOW_JESTER = Identifier.of("noellesroles", "shadow_jester");
    public static final Identifier NOELLES_JESTER = Identifier.of("noellesroles", "jester");
    /**
     * Inferred cause for NoellesRoles' unscoped Shadow Jester to Jester transform.
     * NoellesRoles 未开启原因作用域的“影子小丑化身小丑”所推断的原因。
     */
    public static final Identifier NOELLES_SHADOW_TRANSFORM = Identifier.of("noellesroles", "shadow_transform");

    private RoleChangeRecordRules() {
    }

    /**
     * Only live server writes during a running, recorded match count. Opening assignment runs before Wathe starts
     * the match record; save loads and client syncs replay roles through {@code readFromNbt}.
     * 仅统计对局进行中、已开始记录时的服务端实时写入。开局分配早于 Wathe 开始记录；存档加载与客户端同步经由
     * {@code readFromNbt} 重放身份，不计入。
     */
    public static boolean isRecordingContext(
            boolean serverWorld,
            boolean restoringFromNbt,
            boolean activeMatch,
            boolean gameRunning
    ) {
        return serverWorld && !restoringFromNbt && activeMatch && gameRunning;
    }

    /** A real change has a new role and a different effective role id. 真实变化须有新身份且有效身份标识不同。 */
    public static boolean isRoleChange(@Nullable Identifier from, @Nullable Identifier to) {
        return to != null && !Objects.equals(effective(from), effective(to));
    }

    public static boolean shouldRecord(
            boolean serverWorld,
            boolean restoringFromNbt,
            boolean activeMatch,
            boolean gameRunning,
            @Nullable Identifier from,
            @Nullable Identifier to
    ) {
        return isRecordingContext(serverWorld, restoringFromNbt, activeMatch, gameRunning) && isRoleChange(from, to);
    }

    /** The {@code from} value to persist; absent and NO_ROLE are omitted. 需写入的 from；无身份与 NO_ROLE 不写。 */
    public static @Nullable Identifier recordedFrom(@Nullable Identifier from) {
        return effective(from);
    }

    /**
     * An open {@code withRoleChangeCause} scope wins; otherwise known unscoped conversions are inferred.
     * 已开启的 {@code withRoleChangeCause} 作用域优先；否则推断已知的未标注转化。
     */
    public static @Nullable RoleChangeCauseScope.Cause resolveCause(
            @Nullable RoleChangeCauseScope.Cause scoped,
            @Nullable Identifier from,
            @Nullable Identifier to
    ) {
        if (scoped != null) {
            return scoped;
        }
        if (NOELLES_SHADOW_JESTER.equals(from) && NOELLES_JESTER.equals(to)) {
            return new RoleChangeCauseScope.Cause(NOELLES_SHADOW_TRANSFORM, null);
        }
        return null;
    }

    /**
     * True when {@code events} holds a role-changed record for {@code player} to {@code to} at {@code worldTick}.
     * 当 {@code events} 中存在该玩家在同一世界刻变为 {@code to} 的身份变化记录时返回 true。
     */
    public static boolean containsRoleChange(List<GameRecordEvent> events, UUID player, Identifier to, long worldTick) {
        String target = to.toString();
        for (GameRecordEvent event : events) {
            if (event.worldTick() != worldTick || !SparkReplayApi.ROLE_CHANGED_EVENT_TYPE.equals(event.type())) {
                continue;
            }
            NbtCompound data = event.data();
            if (data.containsUuid(ReplayRoleTimeline.KEY_PLAYER)
                    && player.equals(data.getUuid(ReplayRoleTimeline.KEY_PLAYER))
                    && target.equals(data.getString(ReplayRoleTimeline.KEY_TO))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Identifier effective(@Nullable Identifier role) {
        return WATHE_NO_ROLE.equals(role) ? null : role;
    }
}
