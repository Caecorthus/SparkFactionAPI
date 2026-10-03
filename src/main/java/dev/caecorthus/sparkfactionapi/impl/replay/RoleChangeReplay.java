package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesShadowTransformReplayCompat;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;

/**
 * Recording side of the replay module: role-change events, their formatter, and compat rules.
 * 回放模块的记录侧：身份变化事件、其格式化器与兼容规则。
 */
public final class RoleChangeReplay {
    private RoleChangeReplay() {
    }

    public static void register() {
        ReplayRegistry.registerFormatter(SparkReplayApi.ROLE_CHANGED_EVENT_TYPE, RoleChangedReplayFormatter::format);
        NoellesShadowTransformReplayCompat.register();
    }
}
