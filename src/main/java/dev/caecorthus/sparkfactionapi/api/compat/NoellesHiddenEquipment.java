package dev.caecorthus.sparkfactionapi.api.compat;

import dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesHiddenEquipmentRegistry;
import net.minecraft.item.Item;

/**
 * Registers held items for NoellesRoles' existing equipment-hiding path when that mod is present.
 * 当 NoellesRoles 存在时，将手持物品注册到其现有的装备隐藏流程。
 */
public final class NoellesHiddenEquipment {
    private NoellesHiddenEquipment() {
    }

    /**
     * Idempotently registers an item by identity; registration is inert when NoellesRoles is absent.
     * 按物品实例幂等注册；NoellesRoles 缺席时，注册保持无副作用。
     */
    public static void register(Item item) {
        NoellesHiddenEquipmentRegistry.register(item);
    }
}
