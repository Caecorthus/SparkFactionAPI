package dev.caecorthus.sparkfactionapi.impl.mapreset;

import dev.caecorthus.sparkfactionapi.component.SparkFactionMapResetComponent;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.World;

/**
 * Side-agnostic reads of the synced lobby map reset progress. The client must hold Wathe's auto-start countdown the
 * same way the server does, or its locally ticked copy would wrap around and show the full countdown again.
 * 同步的大厅地图重置进度的两端通用读取。客户端必须与服务端一样停住 Wathe 的自动开局倒计时，
 * 否则客户端本地 tick 的副本会回绕并重新显示完整倒计时。
 */
public final class LobbyMapResetState {
    private LobbyMapResetState() {
    }

    public static boolean isResetting(World world) {
        SparkFactionMapResetComponent progress = SparkFactionMapResetComponent.KEY.getNullable(world.getScoreboard());
        return progress != null && progress.isResetting();
    }

    /** Wathe {@code AutoStartComponent.tick}: true when this tick must be skipped. / 为 true 时跳过本次 tick。 */
    public static boolean holdsAutoStart(World world, int startTime, int time) {
        if (!isResetting(world)) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        GameMode gameMode = game.getGameMode();
        if (game.getGameStatus() != GameWorldComponent.GameStatus.INACTIVE || gameMode == null) {
            return false;
        }
        // Wathe restarts the countdown during a vote; the hold must not keep it at zero through one.
        // 投票期间 Wathe 会重置倒计时；等待不能让它在投票中一直停在零。
        if (MapVotingComponent.KEY.get(world.getScoreboard()).isVotingActive()) {
            return false;
        }
        // A client may track fewer players in a long ready area than the server sees, so it holds on the synced
        // progress alone; the server's own countdown resets still reach it as syncs.
        // 客户端在较长的准备区中追踪到的玩家可能少于服务端，因此只按同步进度等待；服务端自身的倒计时重置仍会同步过来。
        int readyPlayers = world.isClient() ? gameMode.minPlayerCount : GameFunctions.getReadyPlayerCount(world);
        return LobbyMapResetRules.holdsAutoStart(true, startTime, time, readyPlayers, gameMode.minPlayerCount);
    }
}
