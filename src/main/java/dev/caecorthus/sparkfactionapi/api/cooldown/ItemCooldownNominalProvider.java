package dev.caecorthus.sparkfactionapi.api.cooldown;

import net.minecraft.item.Item;

import java.util.OptionalInt;

/**
 * Supplies the full (nominal) cooldown of an item, or empty when this provider does not know it.
 * 提供物品的完整（标准）冷却；不知道时返回空。
 */
@FunctionalInterface
public interface ItemCooldownNominalProvider {
    OptionalInt nominalTicks(Item item);
}
