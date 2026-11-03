package dev.caecorthus.sparkfactionapi.mixin.mapreset;

import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapReset;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses a new map vote while the lobby map reset runs: it would move everyone off the map about to start. Backs up
 * the command refusal for paths that never reach {@code CommandManager.execute}, such as datapack functions. Wathe
 * itself only votes after a round.
 * 大厅地图重置期间拒绝新的地图投票：它会把所有人带离即将开局的地图。为不经过 {@code CommandManager.execute} 的途径
 * （如数据包函数）补上命令拒绝。Wathe 自身只在对局结束后发起投票。
 */
@Mixin(value = MapVotingComponent.class, remap = false)
public abstract class MapVotingComponentMapResetMixin {
    @Inject(method = "startVoting()V", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$refuseVoteDuringMapReset(CallbackInfo ci) {
        if (LobbyMapReset.refuseVote()) {
            ci.cancel();
        }
    }
}
