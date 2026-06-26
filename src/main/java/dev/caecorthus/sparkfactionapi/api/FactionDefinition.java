package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Defines a first-class custom faction.
 * 定义一个第一类自定义阵营。
 */
public record FactionDefinition(
        Identifier id,
        int color,
        String translationKeyPrefix,
        FactionCapabilities capabilities,
        FactionAssignmentPhase assignmentPhase,
        int priority,
        FactionAssignmentPolicy assignmentPolicy,
        FactionWinCondition winCondition
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

        public FactionDefinition build() {
            return new FactionDefinition(
                    id,
                    color,
                    translationKeyPrefix,
                    capabilities,
                    assignmentPhase,
                    priority,
                    assignmentPolicy,
                    winCondition
            );
        }
    }
}
