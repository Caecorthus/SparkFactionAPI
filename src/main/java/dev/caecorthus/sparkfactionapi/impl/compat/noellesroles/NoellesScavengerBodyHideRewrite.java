package dev.caecorthus.sparkfactionapi.impl.compat.noellesroles;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Points the two {@code role.getFaction()} reads of NoellesRoles' Scavenger body-hide handler at
 * {@link NoellesNeutralPerkRules#neutralPerkFactionOf}, so neutral-aligned native factions still see hidden bodies
 * (contract D3).
 * <p>
 * Why a class-node rewrite and not an injector: NoellesRoles' check is the {@code @Inject} handler
 * {@code ScavengerBodyHideMixin.hideScavengerBodies}, merged into Wathe's {@code PlayerBodyEntityRenderer} under a
 * per-session unique name ({@code handler$<uid>$noellesroles$hideScavengerBodies}). Mixin selectors need that exact
 * name (regex selectors skip merged methods), and injection points are resolved before any injector runs, so neither
 * the handler nor the call to it can be targeted. SparkFactionAPI's client mixin plugin therefore calls
 * {@link #rewrite} from {@code postApply}, after every mixin, NoellesRoles' included, has been applied to that class.
 * The handler is found by its {@code @MixinMerged(mixin = ...)} owner and original name, never by the unique part.
 * <p>
 * Pinned to noellesroles-1.7.6-h1.5.6-spark (sha256 fcb0da69...): exactly one such handler whose every
 * {@code getFaction()} read is compared with {@code Faction.KILLER} or {@code Faction.NEUTRAL}. Any other shape throws,
 * failing class loading at startup like a stale {@code require = 1} selector instead of silently dropping the perk.
 * Only ASM and string names are used here, so running it inside the transformer never loads Wathe classes.
 * 将 NoellesRoles 清道夫尸体隐藏处理器中的两次 {@code role.getFaction()} 读取改为调用
 * {@link NoellesNeutralPerkRules#neutralPerkFactionOf}，让中立同盟的原生阵营仍能看到隐藏尸体（契约 D3）。
 * 为何改写类节点而非注入：该判定是 {@code ScavengerBodyHideMixin.hideScavengerBodies} 这个 {@code @Inject} 处理器，
 * 以每次会话唯一的名字（{@code handler$<uid>$noellesroles$hideScavengerBodies}）合并进 Wathe 的
 * {@code PlayerBodyEntityRenderer}。Mixin 选择器需要确切名字（正则选择器会跳过合并方法），注入点又在任何注入器运行前
 * 解析，因此处理器本身和对它的调用都无法作为目标。于是由 SparkFactionAPI 客户端 mixin 插件在 {@code postApply}
 * （该类的所有 mixin，含 NoellesRoles 的，均已应用之后）调用 {@link #rewrite}。处理器按 {@code @MixinMerged(mixin = ...)}
 * 归属和原始名字查找，从不依赖唯一部分。固定到 noellesroles-1.7.6-h1.5.6-spark（sha256 fcb0da69...）：恰好一个这样的
 * 处理器，且每次 {@code getFaction()} 读取都与 {@code Faction.KILLER} 或 {@code Faction.NEUTRAL} 比较。其它形状会抛出异常，
 * 像过期的 {@code require = 1} 选择器一样在启动时让类加载失败，而不是静默丢失福利。此处只用 ASM 与字符串名字，
 * 因此在转换器内运行不会加载 Wathe 类。
 */
public final class NoellesScavengerBodyHideRewrite {
    public static final String NOELLES_MIXIN = "org.agmas.noellesroles.client.mixin.scavenger.ScavengerBodyHideMixin";
    public static final String HANDLER_NAME = "hideScavengerBodies";
    public static final String TARGET_CLASS = "dev/doctor4t/wathe/client/render/entity/PlayerBodyEntityRenderer";
    public static final String RULES_OWNER = "dev/caecorthus/sparkfactionapi/impl/compat/noellesroles/NoellesNeutralPerkRules";
    public static final String RULE_NAME = "neutralPerkFactionOf";

    private static final String ROLE = "dev/doctor4t/wathe/api/Role";
    private static final String FACTION = "dev/doctor4t/wathe/api/Faction";
    private static final String GET_FACTION_DESC = "()L" + FACTION + ";";
    public static final String RULE_DESC = "(L" + ROLE + ";)L" + FACTION + ";";
    private static final String MIXIN_MERGED_DESC = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";

    private NoellesScavengerBodyHideRewrite() {
    }

    /**
     * Rewrites the handler in place and returns how many reads were changed (0 when it was already rewritten).
     * 原地改写处理器，返回改写的读取次数（已改写过时为 0）。
     *
     * @throws IllegalStateException when the class does not hold exactly one NoellesRoles handler of the pinned shape
     *                               类中不是恰好一个固定形状的 NoellesRoles 处理器时
     */
    public static int rewrite(ClassNode target) {
        List<MethodNode> handlers = new ArrayList<>();
        for (MethodNode method : target.methods) {
            if (isNoellesHandler(method)) {
                handlers.add(method);
            }
        }
        if (handlers.size() != 1) {
            throw stale(target, "expected 1 merged " + HANDLER_NAME + " handler, found " + handlers.size());
        }
        MethodNode handler = handlers.getFirst();
        int rewritten = 0;
        int alreadyRewritten = 0;
        for (AbstractInsnNode insn : handler.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode call)) {
                continue;
            }
            if (isRuleCall(call)) {
                alreadyRewritten++;
            } else if (isGetFaction(call)) {
                if (!isPerkGate(call)) {
                    throw stale(target, "a getFaction() read in " + handler.name + " is not a KILLER/NEUTRAL comparison");
                }
                handler.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, RULES_OWNER, RULE_NAME, RULE_DESC, false));
                rewritten++;
            }
        }
        if (rewritten + alreadyRewritten == 0) {
            throw stale(target, handler.name + " no longer reads Role#getFaction()");
        }
        return rewritten;
    }

    /** NoellesRoles' merged handler: owned by its mixin and named after its original method. */
    public static boolean isNoellesHandler(MethodNode method) {
        if (!method.name.equals(HANDLER_NAME) && !method.name.endsWith("$" + HANDLER_NAME)) {
            return false;
        }
        Object owner = mergedMixin(method.visibleAnnotations);
        if (owner == null) {
            owner = mergedMixin(method.invisibleAnnotations);
        }
        return owner instanceof String name && name.replace('/', '.').equals(NOELLES_MIXIN);
    }

    private static Object mergedMixin(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (MIXIN_MERGED_DESC.equals(annotation.desc) && annotation.values != null) {
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if ("mixin".equals(annotation.values.get(i))) {
                        return annotation.values.get(i + 1);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isGetFaction(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals(ROLE)
                && call.name.equals("getFaction") && call.desc.equals(GET_FACTION_DESC);
    }

    private static boolean isRuleCall(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKESTATIC && call.owner.equals(RULES_OWNER)
                && call.name.equals(RULE_NAME) && call.desc.equals(RULE_DESC);
    }

    // getFaction(); getstatic Faction.KILLER|NEUTRAL; if_acmp(eq|ne)
    private static boolean isPerkGate(MethodInsnNode call) {
        AbstractInsnNode constant = nextReal(call);
        AbstractInsnNode compare = constant == null ? null : nextReal(constant);
        return constant instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                && field.owner.equals(FACTION) && (field.name.equals("KILLER") || field.name.equals("NEUTRAL"))
                && compare != null
                && (compare.getOpcode() == Opcodes.IF_ACMPEQ || compare.getOpcode() == Opcodes.IF_ACMPNE);
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode from) {
        AbstractInsnNode insn = from.getNext();
        while (insn != null && insn.getOpcode() < 0) {
            insn = insn.getNext();
        }
        return insn;
    }

    private static IllegalStateException stale(ClassNode target, String detail) {
        return new IllegalStateException("SparkFactionAPI cannot keep the NoellesRoles Scavenger hidden-body perk for "
                + "neutral-aligned native factions in " + target.name + ": " + detail + ". The rewrite is pinned to "
                + "noellesroles-1.7.6-h1.5.6-spark; re-check NoellesScavengerBodyHideRewrite against the installed "
                + "NoellesRoles. SparkFactionAPI 无法为中立同盟原生阵营保留 NoellesRoles 清道夫隐藏尸体福利。");
    }
}
