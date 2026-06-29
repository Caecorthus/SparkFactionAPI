package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleAppearanceCondition;
import net.minecraft.util.Identifier;

import java.util.Objects;

public record FactionRoleDefinition(
        Identifier roleId,
        Identifier factionId,
        int color,
        Role.MoodType moodType,
        int maxSprintTime,
        boolean canSeeTime,
        RoleAppearanceCondition appearanceCondition,
        Faction nativeWatheFaction
) {
    public FactionRoleDefinition {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(factionId, "factionId");
        moodType = moodType == null ? Role.MoodType.NONE : moodType;
        appearanceCondition = appearanceCondition == null ? RoleAppearanceCondition.ALWAYS : appearanceCondition;
        nativeWatheFaction = nativeWatheFaction == null ? Faction.NONE : nativeWatheFaction;
    }

    public static Builder builder(Identifier roleId, Identifier factionId) {
        return new Builder(roleId, factionId);
    }

    public static final class Builder {
        private final Identifier roleId;
        private final Identifier factionId;
        private int color = 0xFFFFFF;
        private Role.MoodType moodType = Role.MoodType.NONE;
        private int maxSprintTime = -1;
        private boolean canSeeTime;
        private RoleAppearanceCondition appearanceCondition = RoleAppearanceCondition.ALWAYS;
        private Faction nativeWatheFaction = Faction.NONE;

        private Builder(Identifier roleId, Identifier factionId) {
            this.roleId = roleId;
            this.factionId = factionId;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder moodType(Role.MoodType moodType) {
            this.moodType = moodType;
            return this;
        }

        public Builder maxSprintTime(int maxSprintTime) {
            this.maxSprintTime = maxSprintTime;
            return this;
        }

        public Builder canSeeTime(boolean canSeeTime) {
            this.canSeeTime = canSeeTime;
            return this;
        }

        public Builder appearanceCondition(RoleAppearanceCondition appearanceCondition) {
            this.appearanceCondition = appearanceCondition;
            return this;
        }

        public Builder nativeWatheFaction(Faction nativeWatheFaction) {
            this.nativeWatheFaction = nativeWatheFaction;
            return this;
        }

        public FactionRoleDefinition build() {
            return new FactionRoleDefinition(
                    roleId,
                    factionId,
                    color,
                    moodType,
                    maxSprintTime,
                    canSeeTime,
                    appearanceCondition,
                    nativeWatheFaction
            );
        }
    }
}
