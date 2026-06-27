package dev.caecorthus.sparkfactionapi.mixin;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes Wathe shop cooldown storage for additive custom-faction compatibility.
 * 暴露 wathe 商店冷却存储，用于追加式自定义阵营兼容逻辑。
 */
@Mixin(PlayerShopComponent.class)
public interface PlayerShopComponentMixin {
    @Accessor("cooldowns")
    Map<String, Integer> sparkfactionapi$getCooldowns();
}
