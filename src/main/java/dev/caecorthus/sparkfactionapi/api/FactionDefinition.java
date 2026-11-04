package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Defines a first-class custom faction.
 * 定义一个第一类自定义阵营。
 *
 * @param neutralAligned          only for a native Wathe faction (declared under
 *                                {@code sparkfactionapi:native_wathe_factions}): its roles count as neutral for
 *                                {@link SparkFactionApi#isNeutralAligned}; ignored with a warning elsewhere.
 *                                仅用于原生 Wathe 阵营：其角色在 {@link SparkFactionApi#isNeutralAligned} 中视为中立；
 *                                其它阵营设置时记录警告并忽略。
 * @param dealtInWatheNeutralPool only for a native Wathe faction: Wathe's neutral pass reads its roles as NEUTRAL
 *                                (drawn with neutral odds, taking a neutral slot), nowhere else; registering it on any
 *                                other faction throws {@link IllegalArgumentException}.
 *                                仅用于原生 Wathe 阵营：只有 Wathe 中立分配把其角色读作 NEUTRAL（按中立概率抽取并占用
 *                                中立名额）；在其它阵营上注册会抛出 {@link IllegalArgumentException}。
 */
public record FactionDefinition(
        Identifier id,
        int color,
        String translationKeyPrefix,
        FactionCapabilities capabilities,
        FactionAssignmentPhase assignmentPhase,
        int priority,
        FactionAssignmentPolicy assignmentPolicy,
        FactionWinCondition winCondition,
        boolean neutralAligned,
        boolean dealtInWatheNeutralPool
) {
    public FactionDefinition {
        Objects.requireNonNull(id, "id");
        translationKeyPrefix = translationKeyPrefix == null || translationKeyPrefix.isBlank()
                ? "faction." + id.getNamespace() + "." + id.getPath().replace('/', '.')
                : translationKeyPrefix;
        capabilities = capabilities == null ? FactionCapabilities.none() : capabilities;
        assignmentPhase = assignmentPhase == null ? FactionAssignmentPhase.BEFORE_CIVILIANS : assignmentPhase;
        assignmentPolicy = assignmentPolicy == null ? FactionAssignmentPolicy.NONE : assignmentPolicy;
        winCondition = winCondition == null ? FactionWinCondition.none() : winCondition;
    }

    /**
     * Pre-0.1.6.0 canonical constructor, kept for binary compatibility; both native flags are false.
     * 0.1.6.0 之前的规范构造器，为二进制兼容保留；两个原生标记均为 false。
     */
    public FactionDefinition(
            Identifier id,
            int color,
            String translationKeyPrefix,
            FactionCapabilities capabilities,
            FactionAssignmentPhase assignmentPhase,
            int priority,
            FactionAssignmentPolicy assignmentPolicy,
            FactionWinCondition winCondition
    ) {
        this(id, color, translationKeyPrefix, capabilities, assignmentPhase, priority, assignmentPolicy, winCondition,
                false, false);
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final Identifier id;
        private int color = 0xFFFFFF;
        private String translationKeyPrefix;
        private FactionCapabilities capabilities = FactionCapabilities.none();
        private FactionAssignmentPhase assignmentPhase = FactionAssignmentPhase.BEFORE_CIVILIANS;
        private int priority;
        private FactionAssignmentPolicy assignmentPolicy = FactionAssignmentPolicy.NONE;
        private FactionWinCondition winCondition = FactionWinCondition.none();
        private boolean neutralAligned;
        private boolean dealtInWatheNeutralPool;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder translationKeyPrefix(String translationKeyPrefix) {
            this.translationKeyPrefix = translationKeyPrefix;
            return this;
        }

        public Builder capabilities(FactionCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder assignmentPhase(FactionAssignmentPhase assignmentPhase) {
            this.assignmentPhase = assignmentPhase;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder assignmentPolicy(FactionAssignmentPolicy assignmentPolicy) {
            this.assignmentPolicy = assignmentPolicy;
            return this;
        }

        public Builder winCondition(FactionWinCondition winCondition) {
            this.winCondition = winCondition;
            return this;
        }

        /**
         * Native factions only: roles of this faction count as neutral for {@link SparkFactionApi#isNeutralAligned}.
         * 仅限原生阵营：该阵营的角色在 {@link SparkFactionApi#isNeutralAligned} 中视为中立。
         */
        public Builder neutralAligned(boolean neutralAligned) {
            this.neutralAligned = neutralAligned;
            return this;
        }

        /**
         * Native factions only: Wathe's neutral pass deals roles of this faction like NEUTRAL roles.
         * 仅限原生阵营：Wathe 中立分配像对待中立角色一样发放该阵营的角色。
         */
        public Builder dealtInWatheNeutralPool(boolean dealtInWatheNeutralPool) {
            this.dealtInWatheNeutralPool = dealtInWatheNeutralPool;
            return this;
        }

        public FactionDefinition build() {
            return new FactionDefinition(
                    id,
                    color,
                    translationKeyPrefix,
                    capabilities,
                    assignmentPhase,
                    priority,
                    assignmentPolicy,
                    winCondition,
                    neutralAligned,
                    dealtInWatheNeutralPool
            );
        }
    }
}
