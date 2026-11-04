package dev.caecorthus.sparkfactionapi.client.mixin.compat.noellesroles;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Empty anchor on Wathe's {@code PlayerBodyEntityRenderer}: it only makes Mixin call
 * {@code SparkFactionClientMixinPlugin.postApply} for that class, which runs {@code NoellesScavengerBodyHideRewrite}
 * on NoellesRoles' merged Scavenger body-hide handler (contract D3; see that class for why an injector cannot reach
 * the handler). It adds nothing to the class, and the plugin skips it when NoellesRoles is absent.
 * Wathe {@code PlayerBodyEntityRenderer} 上的空锚点：只为让 Mixin 对该类调用 {@code SparkFactionClientMixinPlugin.postApply}，
 * 由其对 NoellesRoles 合并后的清道夫尸体隐藏处理器执行 {@code NoellesScavengerBodyHideRewrite}（契约 D3；为何注入器无法
 * 触及该处理器见该类说明）。它不向类添加任何内容；未安装 NoellesRoles 时插件跳过它。
 */
@Mixin(value = PlayerBodyEntityRenderer.class, remap = false)
public abstract class NoellesScavengerBodyHideNeutralPerkMixin {
}
