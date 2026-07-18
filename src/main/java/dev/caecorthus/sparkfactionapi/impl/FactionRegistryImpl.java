package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.EffectiveFactionResolver;
import dev.caecorthus.sparkfactionapi.api.FactionBlackoutCooldownPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionEconomyPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionGunPunishmentPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionTargetEligibility;
import dev.caecorthus.sparkfactionapi.api.PlayerAffectPolicy;
import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutCooldownPolicies;
import dev.caecorthus.sparkfactionapi.impl.economy.FactionEconomyPolicies;
import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentPolicies;
import dev.caecorthus.sparkfactionapi.impl.registry.EffectiveFactionResolvers;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionCatalog;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionRegistryBootstrap;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionRoleCatalog;
import dev.caecorthus.sparkfactionapi.impl.target.FactionTargetPolicies;
import dev.caecorthus.sparkfactionapi.impl.target.FactionTargetRules;
import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectPolicies;
import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectRules;
import dev.caecorthus.sparkfactionapi.impl.vision.FactionInstinctPolicies;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class FactionRegistryImpl {
    private FactionRegistryImpl() {
    }

    public static void bootstrap() {
        FactionRegistryBootstrap.bootstrap();
    }

    public static FactionDefinition registerFaction(FactionDefinition definition) {
        return FactionCatalog.registerFaction(definition);
    }

    public static Role registerRole(FactionRoleDefinition definition) {
        return FactionRoleCatalog.registerRole(definition);
    }

    public static boolean isSparkFactionRole(Role role) {
        return FactionRoleCatalog.isSparkFactionRole(role);
    }

    public static Optional<Faction> nativeFactionOverride(Role role) {
        return FactionRoleCatalog.nativeFactionOverride(role);
    }

    public static Optional<Boolean> nativeNeutralOverride(Role role) {
        return FactionRoleCatalog.nativeNeutralOverride(role);
    }

    public static Identifier resolveBaseFaction(Role role) {
        return FactionRoleCatalog.resolveBaseFaction(role);
    }

    public static Identifier resolveEffectiveFaction(PlayerEntity player, GameWorldComponent gameComponent) {
        return EffectiveFactionResolvers.resolve(player, gameComponent);
    }

    public static boolean canTarget(
            PlayerEntity viewer,
            PlayerEntity target,
            Identifier targetTag,
            GameWorldComponent gameComponent
    ) {
        bootstrap();
        return FactionTargetRules.canTarget(viewer, target, targetTag, gameComponent);
    }

    public static boolean canAffectPlayer(
            PlayerEntity actor,
            PlayerEntity target,
            Identifier actionId,
            GameWorldComponent gameComponent
    ) {
        bootstrap();
        return PlayerAffectRules.canAffectPlayer(actor, target, actionId, gameComponent);
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        return FactionCatalog.capabilities(factionId);
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        return FactionCatalog.getFaction(factionId);
    }

    public static Collection<FactionDefinition> getCustomFactions() {
        return FactionCatalog.getCustomFactions();
    }

    public static boolean isCustomFaction(Identifier factionId) {
        return FactionCatalog.isCustomFaction(factionId);
    }

    public static Collection<Role> getRolesForFaction(Identifier factionId) {
        return FactionRoleCatalog.getRolesForFaction(factionId);
    }

    public static List<FactionTargetEligibility> targetEligibility() {
        return FactionTargetPolicies.targetEligibility();
    }

    public static List<FactionEconomyPolicy> economyPolicies() {
        return FactionEconomyPolicies.economyPolicies();
    }

    public static List<FactionGunPunishmentPolicy> gunPunishmentPolicies() {
        return FactionGunPunishmentPolicies.gunPunishmentPolicies();
    }

    public static List<FactionBlackoutCooldownPolicy> blackoutCooldownPolicies() {
        return FactionBlackoutCooldownPolicies.blackoutCooldownPolicies();
    }

    public static List<FactionInstinctPolicy> instinctPolicies() {
        return FactionInstinctPolicies.instinctPolicies();
    }

    public static void registerEffectiveFactionResolver(EffectiveFactionResolver resolver) {
        EffectiveFactionResolvers.register(resolver);
    }

    public static void registerTargetEligibility(FactionTargetEligibility eligibility) {
        FactionTargetPolicies.register(eligibility);
    }

    public static void registerPlayerAffectPolicy(PlayerAffectPolicy policy) {
        PlayerAffectPolicies.register(policy);
    }

    public static void registerEconomyPolicy(FactionEconomyPolicy policy) {
        FactionEconomyPolicies.register(policy);
    }

    public static void registerGunPunishmentPolicy(FactionGunPunishmentPolicy policy) {
        FactionGunPunishmentPolicies.register(policy);
    }

    public static void registerBlackoutCooldownPolicy(FactionBlackoutCooldownPolicy policy) {
        FactionBlackoutCooldownPolicies.register(policy);
    }

    public static void registerInstinctPolicy(FactionInstinctPolicy policy) {
        FactionInstinctPolicies.register(policy);
    }

}
