package dev.caecorthus.sparkfactionapi.mixin;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheRoles;
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

import java.util.List;
import java.util.Map;
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
        if (!customRoundEnd.hasCustomWin()) {
            return;
        }

        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        this.gameMode = game.getGameMode() != null ? game.getGameMode().identifier : null;

        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            GameProfile profile = game.getGameProfiles().get(uuid);
            if (profile == null || role == WatheRoles.NO_ROLE) {
                continue;
            }

            boolean wasDead = game.isPlayerDead(uuid);
            boolean isOnline = serverWorld.getPlayerByUuid(uuid) != null;
            GameRoundEndComponent.PlayerEndStatus endStatus = sparkfactionapi$endStatus(wasDead, isOnline);
            boolean isWinner = customRoundEnd.didWin(uuid);
            this.players.add(new GameRoundEndComponent.RoundEndData(profile, role.identifier(), endStatus, isWinner));
        }

        this.winStatus = winStatus;
        this.sync();
        ci.cancel();
    }

    @Inject(method = "didWin", at = @At("HEAD"), cancellable = true)
    private void sparkfactionapi$didWinFromCustomRoundEnd(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        SparkFactionRoundEndComponent customRoundEnd = SparkFactionRoundEndComponent.KEY.get(this.scoreboard);
        if (customRoundEnd.hasCustomWin()) {
            cir.setReturnValue(customRoundEnd.didWin(uuid));
        }
    }

    private static GameRoundEndComponent.PlayerEndStatus sparkfactionapi$endStatus(boolean wasDead, boolean isOnline) {
        if (wasDead) {
            return isOnline
                    ? GameRoundEndComponent.PlayerEndStatus.DEAD
                    : GameRoundEndComponent.PlayerEndStatus.LEFT_DEAD;
        }
        return isOnline
                ? GameRoundEndComponent.PlayerEndStatus.ALIVE
                : GameRoundEndComponent.PlayerEndStatus.LEFT;
    }
}
