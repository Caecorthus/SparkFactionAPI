package dev.caecorthus.sparkfactionapi.mixin.mapreset;

import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapResetState;
import dev.doctor4t.wathe.cca.AutoStartComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds Wathe's auto-start countdown at zero while the lobby map reset runs. Runs on both sides: the client ticks its
 * own copy of the countdown between syncs.
 * 大厅地图重置期间让 Wathe 自动开局倒计时停在零。两端都会运行：客户端在两次同步之间自行 tick 倒计时副本。
 */
@Mixin(value = AutoStartComponent.class, remap = false)
public abstract class AutoStartComponentMapResetMixin {
    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$waitForMapReset(CallbackInfo ci) {
        AutoStartComponent autoStart = (AutoStartComponent) (Object) this;
        if (LobbyMapResetState.holdsAutoStart(autoStart.world, autoStart.startTime, autoStart.time)) {
            ci.cancel();
        }
    }
}
