package dev.caecorthus.sparkfactionapi.impl.cooldown;

import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownKind;
import dev.caecorthus.sparkfactionapi.api.cooldown.CooldownSlot;
import dev.caecorthus.sparkfactionapi.api.cooldown.ItemCooldownNominalProvider;
import dev.caecorthus.sparkfactionapi.api.cooldown.RoleSkillCooldownStore;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

/** G0 stub; WP-B implements it. / G0 桩，由 WP-B 实现。 */
public final class ForcedCooldownRegistry {
    private ForcedCooldownRegistry() {
    }

    public static void registerRoleSkillStore(RoleSkillCooldownStore store) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static void registerItemNominalProvider(ItemCooldownNominalProvider provider) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static void registerItemNominal(Item item, int ticks) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static void registerItemExemption(Predicate<Item> exemption) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static List<CooldownSlot> slots(ServerPlayerEntity player) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static List<CooldownSlot> slots(ServerPlayerEntity player, CooldownKind kind) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static boolean raise(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static boolean extend(ServerPlayerEntity player, CooldownSlot slot, int ticks) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static int raiseAll(ServerPlayerEntity player, CooldownKind kind, int ticks) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static OptionalInt itemNominalTicks(Item item) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static boolean isItemExempt(Item item) {
        throw new UnsupportedOperationException("WP-B");
    }

    public static int itemRemainingTicks(ServerPlayerEntity player, Item item) {
        throw new UnsupportedOperationException("WP-B");
    }
}
