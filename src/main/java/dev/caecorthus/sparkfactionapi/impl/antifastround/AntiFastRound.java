package dev.caecorthus.sparkfactionapi.impl.antifastround;

import dev.caecorthus.sparkfactionapi.component.SparkFactionAntiFastRoundComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Safe-time state reads (both sides) and the server-only window lifecycle. The window opens when Wathe finishes
 * initializing a round, closes on time, on round finalization, or when an administrator disables the feature.
 * 安全时间状态读取（两端通用）以及仅服务端执行的窗口生命周期。Wathe 完成对局初始化时开启窗口，
 * 到时、对局收尾或管理员关闭功能时关闭。
 */
public final class AntiFastRound {
    private static final String START_KEY = "message.sparkfactionapi.anti_fast_round.start";
    private static final String END_KEY = "message.sparkfactionapi.anti_fast_round.end";
    private static final String BLOCKED_KEY = "message.sparkfactionapi.anti_fast_round.blocked";

    private AntiFastRound() {
    }

    /**
     * Side-agnostic: true while the window is open, the player is alive in a running round, and not creative or a
     * spectator. The server compares the authoritative window with the world time. The client treats its synced copy
     * as open until the server's close sync arrives, so a lagging server never sees the client unlock early.
     * 两端通用：窗口开启、玩家在进行中的对局里存活且不是创造或旁观模式时返回 true。服务端用世界时间判断权威窗口；
     * 客户端在收到服务端的关闭同步前一直视同步副本为开启，因此服务端卡顿时客户端也不会提前解锁。
     */
    public static boolean isRestricted(@Nullable PlayerEntity player) {
        if (player == null) {
            return false;
        }
        SparkFactionAntiFastRoundComponent state = state(player.getWorld());
        return state != null
                && isOpen(player.getWorld(), state)
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && GameFunctions.isPlayerPlayingAndAlive(player);
    }

    public static int remainingSeconds(@Nullable PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        SparkFactionAntiFastRoundComponent state = state(player.getWorld());
        if (state == null || !isOpen(player.getWorld(), state)) {
            return 0;
        }
        // A client clock running ahead of the server may already read zero while the lock still holds.
        // 客户端时钟快于服务端时可能已算出零，但锁仍然有效。
        return Math.max(1, AntiFastRoundRules.remainingSeconds(player.getWorld().getTime(), state.getWindowEnd()));
    }

    /**
     * Shows the refusal on the action bar. Works on both sides: the client draws it locally, the server sends it.
     * 在动作栏显示拒绝提示。两端可用：客户端本地显示，服务端发送给玩家。
     */
    public static void notifyBlocked(PlayerEntity player) {
        int remaining = remainingSeconds(player);
        player.sendMessage(Text.translatableWithFallback(
                BLOCKED_KEY,
                "Safe time: items and skills are locked (%s s left)",
                remaining
        ).formatted(Formatting.YELLOW), true);
    }

    /** Server only: Wathe {@code GameEvents.ON_FINISH_INITIALIZE}. / 仅服务端：Wathe 对局初始化完成事件。 */
    static void onRoundInitialized(World world, GameWorldComponent gameComponent) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        SparkFactionAntiFastRoundComponent state = state(serverWorld);
        if (state == null) {
            return;
        }
        if (!state.isEnabled()) {
            state.closeWindow();
            return;
        }
        long now = serverWorld.getTime();
        state.openWindow(serverWorld.getRegistryKey(), now, AntiFastRoundRules.windowEnd(now, state.getSeconds()));
    }

    /** Server only: Wathe {@code GameEvents.ON_FINISH_FINALIZE}. / 仅服务端：Wathe 对局收尾完成事件。 */
    static void onRoundFinalized(World world, GameWorldComponent gameComponent) {
        if (world instanceof ServerWorld serverWorld) {
            SparkFactionAntiFastRoundComponent state = state(serverWorld);
            if (state != null) {
                state.closeWindow();
            }
        }
    }

    /** Server only: sends the delayed start notice and closes the window on time. / 仅服务端：延迟发送开局提示并按时关闭窗口。 */
    static void tick(MinecraftServer server) {
        SparkFactionAntiFastRoundComponent state = SparkFactionAntiFastRoundComponent.KEY.getNullable(server.getScoreboard());
        if (state == null || !state.hasOpenWindow()) {
            return;
        }
        ServerWorld world = state.getWindowWorld() == null ? null : server.getWorld(state.getWindowWorld());
        if (world == null) {
            state.closeWindow();
            return;
        }
        long now = world.getTime();
        if (!AntiFastRoundRules.isWithinWindow(now, state.getWindowEnd())) {
            state.closeWindow();
            broadcast(world, Text.translatableWithFallback(
                    END_KEY,
                    "Safe time is over: items and skills are unlocked"
            ).formatted(Formatting.GREEN));
            return;
        }
        if (!state.isStartNoticeSent()
                && now - state.getWindowStart() >= AntiFastRoundRules.FADE_IN_TICKS) {
            state.markStartNoticeSent();
            broadcast(world, Text.translatableWithFallback(
                    START_KEY,
                    "Safe time: items and skills are locked for %s s, hits deal no knockback",
                    AntiFastRoundRules.remainingSeconds(now, state.getWindowEnd())
            ).formatted(Formatting.YELLOW));
        }
    }

    /**
     * Server only: stores the settings; disabling also ends a running window at once (silently).
     * 仅服务端：保存设置；关闭功能时同时立即（静默）结束正在进行的窗口。
     */
    public static void applySettings(MinecraftServer server, boolean enabled, int seconds) {
        SparkFactionAntiFastRoundComponent state = SparkFactionAntiFastRoundComponent.KEY.get(server.getScoreboard());
        state.setSettings(enabled, seconds);
        if (!enabled) {
            state.closeWindow();
        }
    }

    public static SparkFactionAntiFastRoundComponent settings(MinecraftServer server) {
        return SparkFactionAntiFastRoundComponent.KEY.get(server.getScoreboard());
    }

    private static void broadcast(ServerWorld world, Text message) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(message, true);
        }
    }

    private static boolean isOpen(World world, SparkFactionAntiFastRoundComponent state) {
        return world.isClient()
                ? state.hasOpenWindow()
                : AntiFastRoundRules.isWithinWindow(world.getTime(), state.getWindowEnd());
    }

    private static @Nullable SparkFactionAntiFastRoundComponent state(World world) {
        return SparkFactionAntiFastRoundComponent.KEY.getNullable(world.getScoreboard());
    }
}
