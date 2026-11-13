package dev.caecorthus.sparkfactionapi.api;

@FunctionalInterface
public interface FactionAssignmentPolicy {
    FactionAssignmentPolicy NONE = context -> 0;

    /**
     * Returns the number of players this faction wants in the current phase.
     * 返回此阵营在当前分配阶段希望获得的人数。
     */
    int desiredSlots(FactionAssignmentContext context);
}
