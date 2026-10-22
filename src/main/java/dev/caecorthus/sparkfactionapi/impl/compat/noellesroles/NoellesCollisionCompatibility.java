package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Bridges NoellesRoles' status-effect capability into the shared bilateral collision contract. */
public final class NoellesCollisionCompatibility {
    private static final String MOD_ID = "noellesroles";
    private static final Identifier NO_COLLISION = Identifier.of("noellesroles", "no_collision");
    private static boolean registered;
    // Status effects are a static registry, so a found entry stays valid; a miss is retried on the next call
    // in case the lookup ran before NoellesRoles registered the effect.
    // 状态效果是静态注册表，查到的条目会一直有效；未查到时下次调用会重试，以防查询早于 NoellesRoles 注册该效果。
    private static volatile @Nullable RegistryEntry<StatusEffect> noCollision;

    private NoellesCollisionCompatibility() {
    }

    public static synchronized void register() {
        // Without NoellesRoles the effect id is never registered, so the predicate could only return false.
        // 未安装 NoellesRoles 时该效果永远不会注册，谓词只会返回 false，因此不注册。
        if (registered || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        registered = true;
        SparkFactionApi.registerEntityCollisionExemption(entity -> {
            if (!(entity instanceof LivingEntity livingEntity)) {
                return false;
            }
            RegistryEntry<StatusEffect> effect = noCollisionEffect();
            return effect != null && livingEntity.hasStatusEffect(effect);
        });
    }

    private static @Nullable RegistryEntry<StatusEffect> noCollisionEffect() {
        RegistryEntry<StatusEffect> effect = noCollision;
        if (effect == null) {
            effect = Registries.STATUS_EFFECT.getEntry(NO_COLLISION).orElse(null);
            noCollision = effect;
        }
        return effect;
    }
}
