package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Removes denied players from player-owned projectile collision queries.
 * 从玩家发射物的碰撞查询中移除禁止受影响的玩家。
 */
@Mixin(World.class)
public abstract class WorldProjectileAffectMixin {
    private static final Identifier PROJECTILE_ACTION =
            Identifier.of("sparkfactionapi", "projectile");

    @Inject(method = "getOtherEntities", at = @At("RETURN"), cancellable = true)
    private void sparkfactionapi$filterDeniedProjectileTargets(
            Entity except,
            Box box,
            Predicate<? super Entity> predicate,
            CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (!(except instanceof ProjectileEntity projectile)
                || !(projectile.getOwner() instanceof ServerPlayerEntity actor)) {
            return;
        }

        List<Entity> original = cir.getReturnValue();
        List<Entity> filtered = new ArrayList<>(original.size());
        for (Entity entity : original) {
            if (!(entity instanceof ServerPlayerEntity target)
                    || PlayerAffectMixinGuard.allows(actor, target, PROJECTILE_ACTION)) {
                filtered.add(entity);
            }
        }
        if (filtered.size() != original.size()) {
            cir.setReturnValue(filtered);
        }
    }
}
