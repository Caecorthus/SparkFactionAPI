package dev.caecorthus.sparkfactionapi.mixin.nativefaction;

import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactionDeclarations;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactions;
import dev.doctor4t.wathe.api.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Appends the native Wathe factions declared under {@code sparkfactionapi:native_wathe_factions} to Wathe's
 * {@code Faction} enum. Wathe's {@code <clinit>} ends with {@code $VALUES = $values()}; at TAIL the four built-ins are
 * set, and replacing {@code $VALUES} there means every later {@code values()}, {@code valueOf}, {@code EnumMap} and
 * switch map (all built lazily from {@code values()}) already sees the extended set. The handler must not call
 * {@code Faction.fromRole}, a SparkFactionAPI registry, {@code valueOf} or an {@code EnumMap}/{@code EnumSet}: those
 * would re-enter or cache the class before the write. Invalid declarations fail class init (startup crash, logged).
 * 把 {@code sparkfactionapi:native_wathe_factions} 声明的原生阵营追加到 Wathe 的 {@code Faction} 枚举。Wathe 的
 * {@code <clinit>} 以 {@code $VALUES = $values()} 结尾；在 TAIL 时 4 个内置常量已就绪，此时替换 {@code $VALUES}，
 * 之后所有 {@code values()}、{@code valueOf}、{@code EnumMap} 与 switch 映射（都由 {@code values()} 惰性构建）
 * 看到的都是扩展后的集合。处理器不得调用 {@code Faction.fromRole}、SparkFactionAPI 注册表、{@code valueOf} 或
 * {@code EnumMap}/{@code EnumSet}，否则会在写入前重入或缓存该类。声明无效时类初始化失败（启动崩溃并记录日志）。
 */
@Mixin(value = Faction.class, remap = false)
public abstract class FactionEnumExtensionMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sparkfactionapi$appendNativeFactions(CallbackInfo ci) {
        Faction[] original = Faction.values();
        List<NativeWatheFactionDeclarations.Declaration> plan = NativeWatheFactions.planFromFabricMetadata(original);
        Faction[] created = new Faction[plan.size()];
        for (int i = 0; i < created.length; i++) {
            created[i] = FactionEnumAccessor.sparkfactionapi$newConstant(plan.get(i).constantName(), original.length + i);
        }
        FactionEnumAccessor.sparkfactionapi$setValues(NativeWatheFactions.install(original, plan, created));
    }
}
