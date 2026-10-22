package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectMixinGuard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Removes denied players from player-owned projectile collision queries.
 * 从玩家发射物的碰撞查询中移除禁止受影响的玩家。
 * {@code getOtherEntities} is one of the hottest world queries, so the original list is returned untouched
 * unless a denied player is found; every player in it is still checked in order.
 * {@code getOtherEntities} 是最频繁的世界查询之一，因此只有发现被拒绝的玩家时才复制列表，否则原样返回；
 * 列表中每名玩家仍按顺序检查。
 */
@Mixin(World.class)
public abstract class WorldProjectileAffectMixin {
    private static final Identifier PROJECTILE_ACTION =
            Identifier.of("sparkfactionapi", "projectile");

    @ModifyReturnValue(method = "getOtherEntities", at = @At("RETURN"))
    private List<Entity> sparkfactionapi$filterDeniedProjectileTargets(
            List<Entity> original,
            Entity except,
            Box box,
            Predicate<? super Entity> predicate
    ) {
        if (!(except instanceof ProjectileEntity projectile)
                || !(projectile.getOwner() instanceof ServerPlayerEntity actor)) {
            return original;
        }

        List<Entity> filtered = null;
        int index = 0;
        for (Entity entity : original) {
            boolean allowed = !(entity instanceof ServerPlayerEntity target)
                    || PlayerAffectMixinGuard.allows(actor, target, PROJECTILE_ACTION);
            if (filtered == null) {
                if (!allowed) {
                    filtered = new ArrayList<>(original.size() - 1);
                    filtered.addAll(original.subList(0, index));
                }
            } else if (allowed) {
                filtered.add(entity);
            }
            index++;
        }
        return filtered != null ? filtered : original;
    }
}
