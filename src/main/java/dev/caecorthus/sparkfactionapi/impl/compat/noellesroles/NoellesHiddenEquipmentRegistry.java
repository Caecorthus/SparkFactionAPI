package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class NoellesHiddenEquipmentRegistry {
    private static final Set<Item> HIDDEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());

    private NoellesHiddenEquipmentRegistry() {
    }

    public static void register(Item item) {
        HIDDEN_ITEMS.add(Objects.requireNonNull(item, "item"));
    }

    public static boolean shouldHide(ItemStack stack) {
        return !stack.isEmpty() && HIDDEN_ITEMS.contains(stack.getItem());
    }
}
