package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionAssignmentPhase;
import dev.caecorthus.sparkfactionapi.impl.assignment.FactionAssignmentService;
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionDraw;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        // Third-faction draw (owner 2026-11-16): after every forced/killer/vigilante role, before Wathe's neutral pass.
        // 第三方阵营抽取（所有者 2026-11-16）：在所有强制/杀手/义警角色之后、Wathe 中立分配之前。
        ThirdFactionDraw.drawBeforeNeutrals(world, gameComponent, players);
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
        // Wathe's neutral pass is over: drop the draw's claims so later assignNeutrals calls see the plain pool view.
        // Wathe 中立分配已结束：清除抽取结果，使之后的 assignNeutrals 调用看到普通的中立池视图。
        ThirdFactionDraw.endNeutralPass();
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.AFTER_NEUTRALS);
        FactionAssignmentService.assignPhase(world, gameComponent, players, FactionAssignmentPhase.BEFORE_CIVILIANS);
    }

}
