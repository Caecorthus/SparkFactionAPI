package dev.caecorthus.sparkfactionapi.impl.gun;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.util.GunDropPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Adapts Wathe innocent-shot punishment to explicit custom-faction gun rules.
 * 将 wathe 射击无辜惩罚适配到显式自定义阵营枪罚规则。
 */
public final class FactionGunPunishmentAdapter {
    private FactionGunPunishmentAdapter() {
    }

    public static ShouldPunishGunShooter.PunishResult shouldPunishGunShooter(
            PlayerEntity shooter,
            PlayerEntity victim
    ) {
        if (!(shooter instanceof ServerPlayerEntity serverShooter)) {
            return null;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(shooter.getWorld());
        boolean nativeVictim = gameComponent.isInnocent(victim);
        Boolean victimOverride = FactionGunPunishmentRules.gunPunishmentOverride(
                victim,
                FactionGunPunishmentPolicy.Subject.VICTIM,
                gameComponent
        );
        Boolean shooterOverride = FactionGunPunishmentRules.gunPunishmentOverride(
                shooter,
                FactionGunPunishmentPolicy.Subject.SHOOTER,
                gameComponent
        );
        boolean customVictim = FactionGunPunishmentRules.isCustomPunishableInnocentGunVictim(victim, gameComponent)
                || (!nativeVictim && Boolean.TRUE.equals(victimOverride));
        boolean customShooter = FactionGunPunishmentRules.usesCustomShooterDecision(
                shooter,
                shooterOverride,
                gameComponent
        );

        if (Boolean.FALSE.equals(victimOverride)) {
            return ShouldPunishGunShooter.PunishResult.cancel();
        }
        if (!nativeVictim && !customVictim) {
            return null;
        }
        if (!customVictim && !customShooter) {
            return null;
        }
        if (shooter.isCreative() || !serverShooter.getMainHandStack().isOf(WatheItems.REVOLVER)) {
            return null;
        }

        // Returning null preserves native Wathe, SparkTraits, and NoellesRoles decisions for unrelated cases.
        // 返回 null 可保留无关场景下 wathe、SparkTraits 与 NoellesRoles 的原有判定。
        if (Boolean.FALSE.equals(shooterOverride)
                || (customShooter
                && shooterOverride == null
                && !FactionGunPunishmentRules.isPunishableInnocentGunShooter(shooter, gameComponent))) {
            return ShouldPunishGunShooter.PunishResult.cancel();
        }

        return ShouldPunishGunShooter.PunishResult.custom(() ->
                applyCustomGunPunishment(serverShooter, gameComponent)
        );
    }

    private static void applyCustomGunPunishment(ServerPlayerEntity shooter, GameWorldComponent gameComponent) {
        // Mirrors Wathe's ordinary gun-drop punishment without redirecting its role predicates.
        // 复用 wathe 普通掉枪惩罚语义，但不重定向其角色判断。
        if (!shooter.getInventory().contains(stack -> stack.isIn(WatheItemTags.GUNS))) {
            return;
        }

        Item revolver = WatheItems.REVOLVER;
        shooter.getInventory().remove(stack -> stack.isOf(revolver), 1, shooter.getInventory());
        if (FactionGunPunishmentRules.consumesPunishableGunLikeKiller(shooter, gameComponent)) {
            return;
        }

        ItemEntity droppedGun = shooter.dropItem(revolver.getDefaultStack(), false, false);
        if (droppedGun != null) {
            droppedGun.setPickupDelay(10);
            droppedGun.setThrower(shooter);
        }
        ServerPlayNetworking.send(shooter, new GunDropPayload());
        PlayerMoodComponent.KEY.get(shooter).setMood(0);
        gameComponent.addToPreventGunPickup(shooter);

        if (gameComponent.getShootInnocentPunishment() == GameWorldComponent.ShootInnocentPunishment.KILL_SHOOTER) {
            GameFunctions.killPlayer(shooter, true, null, GameConstants.DeathReasons.SHOT_INNOCENT);
        }
    }
}
