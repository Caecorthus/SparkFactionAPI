package dev.caecorthus.sparkfactionapi.client;

import dev.caecorthus.sparkfactionapi.impl.compat.noellesroles.NoellesScavengerBodyHideRewrite;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Plugin of {@code sparkfactionapi.client.mixins.json}. Every client mixin applies as configured; the only extra
 * behavior is for the anchor {@code NoellesScavengerBodyHideNeutralPerkMixin}: it is skipped without NoellesRoles, and
 * after all mixins were applied to Wathe's {@code PlayerBodyEntityRenderer} its {@code postApply} rewrites
 * NoellesRoles' merged Scavenger body-hide handler ({@link NoellesScavengerBodyHideRewrite}). Mixin classes are named
 * by string here because loading one directly is illegal; this class lives outside the mixin package for the same
 * reason.
 * {@code sparkfactionapi.client.mixins.json} 的插件。所有客户端 mixin 照常应用；唯一的额外行为针对锚点
 * {@code NoellesScavengerBodyHideNeutralPerkMixin}：未安装 NoellesRoles 时跳过它；在 Wathe {@code PlayerBodyEntityRenderer}
 * 应用完所有 mixin 之后，于其 {@code postApply} 改写 NoellesRoles 合并后的清道夫尸体隐藏处理器
 * （{@link NoellesScavengerBodyHideRewrite}）。这里以字符串指代 mixin 类，因为直接加载 mixin 类是非法的；
 * 出于同样原因本类位于 mixin 包之外。
 */
public final class SparkFactionClientMixinPlugin implements IMixinConfigPlugin {
    public static final String SCAVENGER_BODY_HIDE_ANCHOR =
            "dev.caecorthus.sparkfactionapi.client.mixin.compat.noellesroles.NoellesScavengerBodyHideNeutralPerkMixin";
    private static final String NOELLES_ROLES = "noellesroles";
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/ClientMixinPlugin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !SCAVENGER_BODY_HIDE_ANCHOR.equals(mixinClassName) || FabricLoader.getInstance().isModLoaded(NOELLES_ROLES);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!SCAVENGER_BODY_HIDE_ANCHOR.equals(mixinClassName)) {
            return;
        }
        // Throws on an unexpected NoellesRoles shape, failing like a stale require = 1 selector.
        // NoellesRoles 形状不符时抛出异常，与过期的 require = 1 选择器一样失败。
        int rewritten = NoellesScavengerBodyHideRewrite.rewrite(targetClass);
        LOGGER.info("Kept the NoellesRoles Scavenger hidden-body view for neutral-aligned native factions "
                + "({} faction reads in {}). 已为中立同盟原生阵营保留清道夫隐藏尸体可见性。", rewritten, targetClassName);
    }
}
