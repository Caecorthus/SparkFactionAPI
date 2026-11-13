package dev.caecorthus.sparkfactionapi.api;

/**
 * Cross-cutting faction behavior switches.
 * 阵营横切能力开关，默认全部关闭，避免新阵营意外继承 wathe 三阵营行为。
 */
public record FactionCapabilities(
        boolean canUseKillerFeatures,
        boolean receivesKillerPassiveMoney,
        boolean receivesKillRewards,
        boolean isPunishableInnocentGunVictim,
        boolean isPunishableInnocentGunShooter,
        boolean hasBlackoutImmunity,
        boolean sharesCohort,
        boolean canUseInstinct,
        int instinctColor
) {
    public static FactionCapabilities none() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canUseKillerFeatures;
        private boolean receivesKillerPassiveMoney;
        private boolean receivesKillRewards;
        private boolean isPunishableInnocentGunVictim;
        private boolean isPunishableInnocentGunShooter;
        private boolean hasBlackoutImmunity;
        private boolean sharesCohort;
        private boolean canUseInstinct;
        private int instinctColor = -1;

        private Builder() {
        }

        public Builder canUseKillerFeatures(boolean canUseKillerFeatures) {
            this.canUseKillerFeatures = canUseKillerFeatures;
            return this;
        }

        public Builder receivesKillerPassiveMoney(boolean receivesKillerPassiveMoney) {
            this.receivesKillerPassiveMoney = receivesKillerPassiveMoney;
            return this;
        }

        public Builder receivesKillRewards(boolean receivesKillRewards) {
            this.receivesKillRewards = receivesKillRewards;
            return this;
        }

        public Builder isPunishableInnocentGunVictim(boolean isPunishableInnocentGunVictim) {
            this.isPunishableInnocentGunVictim = isPunishableInnocentGunVictim;
            return this;
        }

        public Builder isPunishableInnocentGunShooter(boolean isPunishableInnocentGunShooter) {
            this.isPunishableInnocentGunShooter = isPunishableInnocentGunShooter;
            return this;
        }

        public Builder hasBlackoutImmunity(boolean hasBlackoutImmunity) {
            this.hasBlackoutImmunity = hasBlackoutImmunity;
            return this;
        }

        public Builder sharesCohort(boolean sharesCohort) {
            this.sharesCohort = sharesCohort;
            return this;
        }

        public Builder canUseInstinct(boolean canUseInstinct) {
            this.canUseInstinct = canUseInstinct;
            return this;
        }

        public Builder instinctColor(int instinctColor) {
            this.instinctColor = instinctColor;
            return this;
        }

        public FactionCapabilities build() {
            return new FactionCapabilities(
                    canUseKillerFeatures,
                    receivesKillerPassiveMoney,
                    receivesKillRewards,
                    isPunishableInnocentGunVictim,
                    isPunishableInnocentGunShooter,
                    hasBlackoutImmunity,
                    sharesCohort,
                    canUseInstinct,
                    instinctColor
            );
        }
    }
}
