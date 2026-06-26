package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionAssignmentPhase;
import dev.caecorthus.sparkfactionapi.impl.FactionAssignmentService;
import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MurderGameMode.class, remap = false)
public abstract class MurderGameModeMixin {
    @Inject(
            method = "assignRolesAndGetKillerCount",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/ScoreboardRoleSelectorComponent;assignKillers(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ljava/util/List;I)I"
            )
    )
    private static void sparkfactionapi$assignAfterForcedRoles(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir
    ) {
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.AFTER_FORCED_ROLES);
    }

    @Inject(
            method = "assignRolesAndGetKillerCount",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/ScoreboardRoleSelectorComponent;assignVigilantes(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ljava/util/List;I)V"
            )
    )
    private static void sparkfactionapi$assignAfterKillers(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir
    ) {
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.AFTER_KILLERS);
    }

    @Inject(
            method = "assignRolesAndGetKillerCount",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/ScoreboardRoleSelectorComponent;assignNeutrals(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ljava/util/List;I)I"
            )
    )
    private static void sparkfactionapi$assignAfterVigilantes(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir
    ) {
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.AFTER_VIGILANTES);
    }

    @Inject(
            method = "assignRolesAndGetKillerCount",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/ScoreboardRoleSelectorComponent;assignCivilians(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ljava/util/List;)I"
            )
    )
    private static void sparkfactionapi$assignBeforeCivilians(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir
    ) {
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.AFTER_NEUTRALS);
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.BEFORE_CIVILIANS);
    }

    @Redirect(
            method = "tickServerGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparkfactionapi$usePassiveMoneyCapability(GameWorldComponent gameComponent, PlayerEntity player) {
        return FactionCapabilityBridge.receivesKillerPassiveMoney(player, gameComponent);
    }
}
