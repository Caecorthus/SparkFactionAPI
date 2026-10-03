package dev.caecorthus.sparkfactionapi.mixin.cooldown;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes and (via {@link Mutable}) rewrites the final tick bounds of one vanilla item cooldown entry, so a forced
 * cooldown can be corrected in place after any {@code set} modifiers ran. Server-side writes only.
 * 暴露并（借助 {@link Mutable}）改写单个原版物品冷却条目的 final tick 边界，使强制冷却能在 {@code set} 的倍率执行后
 * 原地校正。仅用于服务端写入。
 */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("startTick")
    int sparkfactionapi$getStartTick();

    @Accessor("endTick")
    int sparkfactionapi$getEndTick();

    @Mutable
    @Accessor("startTick")
    void sparkfactionapi$setStartTick(int startTick);

    @Mutable
    @Accessor("endTick")
    void sparkfactionapi$setEndTick(int endTick);
}
