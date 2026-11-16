package dev.caecorthus.sparkfactionapi.mixin.settlement;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the world a Wathe {@code GameWorldComponent} belongs to, which Wathe keeps private. Conditional settlement
 * exclusions need it to turn the {@code (game, uuid)} pair every settlement point already passes into the online
 * player their condition is asked about. Read-only; the name carries the {@code sparkfactionapi$} prefix so another
 * mod's accessor on the same class cannot collide.
 * 读取 Wathe {@code GameWorldComponent} 所属的世界（Wathe 将其设为私有）。带条件的结算排除需要它，把各结算点已经传入的
 * {@code (game, uuid)} 换成被询问条件的在线玩家。只读；方法名带 {@code sparkfactionapi$} 前缀，避免与其他模组在同一类上
 * 声明的访问器冲突。
 */
@Mixin(value = GameWorldComponent.class, remap = false)
public interface GameWorldComponentWorldAccessor {
    @Accessor("world")
    World sparkfactionapi$getWorld();
}
