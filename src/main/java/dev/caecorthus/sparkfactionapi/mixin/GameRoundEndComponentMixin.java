package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.caecorthus.sparkfactionapi.impl.roundend.FactionRoundEndRows;
import dev.caecorthus.sparkfactionapi.impl.roundend.FactionRoundEndStateRules;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(value = GameRoundEndComponent.class, remap = false)
public abstract class GameRoundEndComponentMixin {
    @Shadow
    @Final
    private Scoreboard scoreboard;

    @Shadow
    @Final
    private List<GameRoundEndComponent.RoundEndData> players;

    @Shadow
    private GameFunctions.WinStatus winStatus;

    @Shadow
    private @Nullable Identifier gameMode;

    @Shadow
    public abstract void sync();

    @Inject(
            method = "setRoundEndData(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparkfactionapi$setCustomFactionRoundEndData(
            ServerWorld serverWorld,
            GameFunctions.WinStatus winStatus,
            CallbackInfo ci
    ) {
        SparkFactionRoundEndComponent customRoundEnd = SparkFactionRoundEndComponent.KEY.get(serverWorld.getScoreboard());
        boolean hasCustomWin = customRoundEnd.hasCustomWin();
        boolean pendingCustomWinWrite = customRoundEnd.hasPendingCustomWinWrite();
        if (FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite(
                hasCustomWin,
                pendingCustomWinWrite,
                winStatus
        )) {
            customRoundEnd.clearCustomWin();
            return;
        }
        if (!FactionRoundEndStateRules.shouldWriteCustomRows(hasCustomWin, pendingCustomWinWrite, winStatus)) {
            return;
        }
        customRoundEnd.markCustomWinWritten();

        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        this.gameMode = FactionRoundEndRows.gameModeId(game);
        this.players.addAll(FactionRoundEndRows.rows(serverWorld, game, customRoundEnd));

        this.winStatus = winStatus;
        this.sync();
        ci.cancel();
    }

    @Inject(
            method = "setRoundEndData(Lnet/minecraft/server/world/ServerWorld;Ljava/util/Collection;)V",
            at = @At("HEAD")
    )
    private void sparkfactionapi$clearCustomFactionRoundEndDataForExplicitWinners(
            ServerWorld serverWorld,
            Collection<UUID> winnerUuids,
            CallbackInfo ci
    ) {
        SparkFactionRoundEndComponent customRoundEnd = SparkFactionRoundEndComponent.KEY.get(serverWorld.getScoreboard());
        if (FactionRoundEndStateRules.shouldClearBeforeWatheExplicitWinnerWrite(customRoundEnd.hasCustomWin())) {
            customRoundEnd.clearCustomWin();
        }
    }

    @Inject(method = "didWin", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$didWinFromCustomRoundEnd(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        SparkFactionRoundEndComponent customRoundEnd = SparkFactionRoundEndComponent.KEY.get(this.scoreboard);
        if (customRoundEnd.hasCustomWin()) {
            cir.setReturnValue(customRoundEnd.didWin(uuid));
        }
    }
}
