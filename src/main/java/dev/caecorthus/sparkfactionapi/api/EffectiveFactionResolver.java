package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface EffectiveFactionResolver {
    /**
     * Returns an effective faction override, or null to keep the current value.
     * 返回有效阵营覆盖；返回 null 表示保留当前结果。
     */
    @Nullable
    Identifier resolve(PlayerEntity player, GameWorldComponent gameComponent, Identifier currentFaction);
}
