package dev.caecorthus.sparkfactionapi.api;

import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.caecorthus.sparkfactionapi.impl.FactionRegistryImpl;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;

public final class SparkFactionApi {
    private SparkFactionApi() {
    }

    public static void bootstrap() {
        FactionRegistryImpl.bootstrap();
    }

    public static FactionDefinition registerFaction(FactionDefinition definition) {
        return FactionRegistryImpl.registerFaction(definition);
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        return FactionRegistryImpl.registerRole(definition);
    }

    public static Identifier resolveBaseFaction(Role role) {
        return FactionRegistryImpl.resolveBaseFaction(role);
    }

    public static Identifier resolveEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionRegistryImpl.resolveEffectiveFaction(player, gameComponent);
    }

    public static Identifier resolveEffectiveFaction(Role role) {
        return FactionRegistryImpl.resolveBaseFaction(role);
    }

    public static boolean canTarget(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        return FactionRegistryImpl.canTarget(viewer, target, targetTag, gameComponent);
    }

    public static boolean hasBlackoutImmunity(Role role) {
        return FactionCapabilityBridge.hasBlackoutImmunity(role);
    }

    public static boolean hasBlackoutImmunity(PlayerEntity player, GameWorldComponent gameComponent) {
        return FactionCapabilityBridge.hasBlackoutImmunity(player, gameComponent);
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        return FactionRegistryImpl.capabilities(factionId);
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        return FactionRegistryImpl.getFaction(factionId);
    }

    public static Collection<FactionDefinition> getCustomFactions() {
        return FactionRegistryImpl.getCustomFactions();
    }

    public static Collection<Role> getRolesForFaction(Identifier factionId) {
        return FactionRegistryImpl.getRolesForFaction(factionId);
    }

    public static void registerEffectiveFactionResolver(EffectiveFactionResolver resolver) {
        FactionRegistryImpl.registerEffectiveFactionResolver(resolver);
    }

    public static void registerTargetEligibility(FactionTargetEligibility eligibility) {
        FactionRegistryImpl.registerTargetEligibility(eligibility);
    }

    public static void registerEconomyPolicy(FactionEconomyPolicy policy) {
        FactionRegistryImpl.registerEconomyPolicy(policy);
    }

    public static void registerGunPunishmentPolicy(FactionGunPunishmentPolicy policy) {
        FactionRegistryImpl.registerGunPunishmentPolicy(policy);
    }

    public static void registerBlackoutCooldownPolicy(FactionBlackoutCooldownPolicy policy) {
        FactionRegistryImpl.registerBlackoutCooldownPolicy(policy);
    }

    public static void registerInstinctPolicy(FactionInstinctPolicy policy) {
        FactionRegistryImpl.registerInstinctPolicy(policy);
    }
}
