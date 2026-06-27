package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.mixin.PlayerShopComponentMixin;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.util.GunDropPayload;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Event-backed compatibility hooks that avoid owning Wathe predicate redirects.
 * 基于事件的兼容钩子，避免独占 wathe 的阵营判断调用点。
 */
public final class FactionCompatibilityEvents {
    private static boolean registered;

    private FactionCompatibilityEvents() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        ShopPurchase.AFTER.register(FactionCompatibilityEvents::afterShopPurchase);
        ShouldPunishGunShooter.EVENT.register(FactionCompatibilityEvents::shouldPunishGunShooter);
    }

    private static void afterShopPurchase(ServerPlayerEntity purchaser, ShopEntry entry, int index, int pricePaid) {
        if (!"blackout".equals(entry.id())) {
            return;
        }

        // Wathe and SparkTraits have already shared native killer cooldowns; add only custom bridges here.
        // wathe 与 SparkTraits 已经分发原生杀手冷却；这里仅追加自定义阵营桥接。
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(purchaser.getWorld());
        int cooldownTicks = GameConstants.getInTicks(5, 0);
        for (ServerPlayerEntity player : purchaser.getServerWorld().getPlayers()) {
            Boolean cooldownOverride = FactionCapabilityBridge.blackoutCooldownOverride(purchaser, player, gameComponent);
            if (cooldownOverride != null) {
                PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
                if (cooldownOverride) {
                    ((PlayerShopComponentMixin) (Object) shop).sparkfactionapi$getCooldowns().put("blackout", cooldownTicks);
                } else {
                    ((PlayerShopComponentMixin) (Object) shop).sparkfactionapi$getCooldowns().remove("blackout");
                }
                shop.sync();
                continue;
            }
            if (!FactionCapabilityBridge.hasCustomKillerFeatureAccess(player, gameComponent)) {
                continue;
            }
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            ((PlayerShopComponentMixin) (Object) shop).sparkfactionapi$getCooldowns().put("blackout", cooldownTicks);
            shop.sync();
        }
    }

    private static ShouldPunishGunShooter.PunishResult shouldPunishGunShooter(
            PlayerEntity shooter,
            PlayerEntity victim
    ) {
        if (!(shooter instanceof ServerPlayerEntity serverShooter)) {
            return null;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(shooter.getWorld());
        boolean nativeVictim = gameComponent.isInnocent(victim);
        Boolean victimOverride = FactionCapabilityBridge.gunPunishmentOverride(
                victim,
                FactionGunPunishmentPolicy.Subject.VICTIM,
                gameComponent
        );
        Boolean shooterOverride = FactionCapabilityBridge.gunPunishmentOverride(
                shooter,
                FactionGunPunishmentPolicy.Subject.SHOOTER,
                gameComponent
        );
        boolean customVictim = FactionCapabilityBridge.isCustomPunishableInnocentGunVictim(victim, gameComponent)
                || (!nativeVictim && Boolean.TRUE.equals(victimOverride));
        boolean customShooter = shooterOverride != null || FactionCapabilityBridge.hasCustomEffectiveFaction(shooter, gameComponent);

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
                && !FactionCapabilityBridge.isPunishableInnocentGunShooter(shooter, gameComponent))) {
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
        if (FactionCapabilityBridge.consumesPunishableGunLikeKiller(shooter, gameComponent)) {
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
