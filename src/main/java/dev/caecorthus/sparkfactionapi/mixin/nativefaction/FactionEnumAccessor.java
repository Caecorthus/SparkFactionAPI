package dev.caecorthus.sparkfactionapi.mixin.nativefaction;

import dev.doctor4t.wathe.api.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes Wathe {@code Faction}'s synthetic {@code $VALUES} array and its private {@code (String name, int ordinal)}
 * enum constructor to {@link FactionEnumExtensionMixin}. {@code @Mutable} drops ACC_FINAL so the setter may write it.
 * Only the extension mixin may call these, and only from {@code Faction.<clinit>}.
 * 向 {@link FactionEnumExtensionMixin} 暴露 Wathe {@code Faction} 的合成字段 {@code $VALUES} 与私有枚举构造器
 * {@code (String name, int ordinal)}。{@code @Mutable} 去掉 ACC_FINAL 以允许写入。只能由扩展 mixin 在
 * {@code Faction.<clinit>} 中调用。
 */
@Mixin(value = Faction.class, remap = false)
public interface FactionEnumAccessor {
    @Accessor("$VALUES")
    @Mutable
    static void sparkfactionapi$setValues(Faction[] values) {
        throw new AssertionError("mixin accessor");
    }

    @Invoker("<init>")
    static Faction sparkfactionapi$newConstant(String name, int ordinal) {
        throw new AssertionError("mixin invoker");
    }
}
