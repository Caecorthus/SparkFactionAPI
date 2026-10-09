package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.doctor4t.wathe.api.event.PsychoModeEvents;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Records {@code sparkfactionapi:psycho} (actor = player, {@code active} bool) when psycho mode really starts or
 * ends. Every psycho source (killer shop, Pig God chase, Serial Killer, Silencer, Jester moment, SparkTraits
 * Depression, ...) goes through Wathe's {@code PlayerPsychoComponent.startPsycho/stopPsycho} or, for SparkStrength's
 * Serial Killer override, fires {@code ON_PSYCHO_START} itself; both events fire on the server only.
 * 当疯魔模式真正开始或结束时记录 {@code sparkfactionapi:psycho}（actor = 玩家，{@code active} 布尔值）。所有疯魔来源
 * （杀手商店、猪神追逐、连环杀手、消音者、小丑时刻、SparkTraits 抑郁……）都经过 Wathe 的
 * {@code PlayerPsychoComponent.startPsycho/stopPsycho}，或（SparkStrength 连环杀手的覆盖实现）自行触发
 * {@code ON_PSYCHO_START}；两个事件都只在服务端触发。
 */
public final class PsychoRecorder {
    public static final String EVENT_TYPE = "sparkfactionapi:psycho";
    public static final String KEY_ACTIVE = "active";

    private static final PsychoEdgeTracker EDGES = new PsychoEdgeTracker();

    private PsychoRecorder() {
    }

    public static void register() {
        PsychoModeEvents.ON_PSYCHO_START.register((player, type) -> onEdge(player, true));
        PsychoModeEvents.ON_PSYCHO_END.register((player, type) -> onEdge(player, false));
    }

    private static void onEdge(ServerPlayerEntity player, boolean active) {
        // Wathe's array-backed invoker stops at a throwing listener: never let one escape.
        // Wathe 的数组事件调用器遇到抛异常的监听器会中断：绝不能让异常逃出。
        try {
            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            if (player == null || match == null || !GameRecordManager.hasActiveMatch()) {
                return;
            }
            boolean edge = active
                    ? EDGES.start(match.getMatchId(), player.getUuid())
                    : EDGES.end(match.getMatchId(), player.getUuid());
            if (edge) {
                GameRecordManager.event(EVENT_TYPE).actor(player).putBool(KEY_ACTIVE, active).record();
            }
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record psycho {} for {}", active ? "start" : "end",
                    player == null ? "<null>" : player.getUuid(), e);
        }
    }
}
