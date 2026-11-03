package dev.caecorthus.sparkfactionapi.mixin.mapreset;

import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapReset;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Starts the lobby map reset the moment the vote countdown ends and a map is picked, so the copy also uses the
 * roulette time. A vote held back for too few players picks nothing and starts nothing.
 * 投票倒计时结束、选定地图的那一刻即开始大厅地图重置，让复制也利用轮盘动画的时间。因人数不足而推迟的投票不会选图，也不会开始重置。
 */
@Mixin(value = MapVotingComponent.class, remap = false)
public abstract class MapVotingComponentMapResetMixin {
    @Shadow
    @Nullable
    private MinecraftServer server;

    /**
     * Backs up the command refusal for paths that never reach {@code CommandManager.execute}, such as datapack
     * functions: a new vote would move everyone off the map being reset. Wathe itself only votes after a round.
     * 为不经过 {@code CommandManager.execute} 的途径（如数据包函数）补上拒绝：新投票会把所有人带离正在重置的地图。
     * Wathe 自身只在对局结束后发起投票。
     */
    @Inject(method = "startVoting()V", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$refuseVoteDuringMapReset(CallbackInfo ci) {
        if (LobbyMapReset.refuseVote()) {
            ci.cancel();
        }
    }

    @Inject(method = "endVoting()V", at = @At("RETURN"))
    private void sparkfactionapi$resetPickedMap(CallbackInfo ci) {
        MapVotingComponent voting = (MapVotingComponent) (Object) this;
        if (server == null || !voting.isRoulettePhase()) {
            return;
        }
        int index = voting.getSelectedMapIndex();
        List<MapVotingComponent.VotingMapEntry> maps = voting.getAvailableMaps();
        if (index >= 0 && index < maps.size()) {
            LobbyMapReset.onMapPicked(server, maps.get(index).dimensionId());
        }
    }
}
