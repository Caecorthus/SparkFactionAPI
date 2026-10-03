package dev.caecorthus.sparkfactionapi.mixin.cooldown;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reads vanilla item cooldown entries so forced cooldowns can be written exactly, bypassing duration modifiers that
 * hook {@code ItemCooldownManager.set}. Names carry the {@code sparkfactionapi$} prefix because SparkWitch,
 * SparkTraits and NoellesRoles declare accessors on the same class.
 * 读取原版物品冷却条目，使强制冷却能越过挂在 {@code ItemCooldownManager.set} 上的时长倍率而精确写入。
 * 方法名带 {@code sparkfactionapi$} 前缀，因为 SparkWitch、SparkTraits 与 NoellesRoles 也在同一类上声明了访问器。
 */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
    /** Values are {@code ItemCooldownManager$Entry}; cast them to {@link ItemCooldownEntryAccessor}. / 值为 Entry，需转为条目访问器。 */
    @Accessor("entries")
    Map<Item, Object> sparkfactionapi$getEntries();

    @Accessor("tick")
    int sparkfactionapi$getTick();
}
