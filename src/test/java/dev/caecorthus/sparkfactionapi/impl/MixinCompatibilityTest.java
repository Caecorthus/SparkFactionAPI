package dev.caecorthus.sparkfactionapi.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinCompatibilityTest {
    @Test
    void mixinsAvoidKnownSparkTraitsRedirectCollisions() throws IOException {
        String mixinConfig = readProjectFile("src/main/resources/sparkfactionapi.mixins.json");
        assertFalse(mixinConfig.contains("GunShootPayloadReceiverMixin"));

        assertNoWathePredicateRedirect(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/PlayerShopComponentMixin.java"
        );
        assertNoWathePredicateRedirect(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModeMixin.java"
        );
        assertNoWathePredicateRedirect(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameFunctionsMixin.java"
        );
    }

    @Test
    void clientMixinsRegisterCustomFactionRoundEndTextOverlay() throws IOException {
        String clientMixinConfig = readProjectFile("src/client/resources/sparkfactionapi.client.mixins.json");

        assertTrue(clientMixinConfig.contains("RoundTextRendererMixin"));
        assertEquals(1, countOccurrences(clientMixinConfig, "\"RoundTextRendererMixin\""));
    }

    @Test
    void gameSettingsCommandHelpersStayOutsideOwnedMixinPackage() throws IOException {
        String gameSettingsMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameSettingsCommandMixin.java"
        );

        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameSettingsCommandRules.java"
        )));
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/GameSettingsCommandRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/command/settings/GameSettingsCommandRules.java"
        )));
        assertTrue(gameSettingsMixin.contains(
                "import dev.caecorthus.sparkfactionapi.command.settings.GameSettingsCommandRules;"
        ));
        assertTrue(gameSettingsMixin.contains("GameSettingsCommandRules.listRolesMessage"));
        assertFalse(gameSettingsMixin.contains("private static Text roleText("));
        assertFalse(gameSettingsMixin.contains("private static Text enabledText("));
        assertFalse(gameSettingsMixin.contains("private static int legacyColor("));
        assertFalse(gameSettingsMixin.contains("private static String legacyFactionKey("));
    }

    @Test
    void compatibilityEventsStayAsEventWiringOnly() throws IOException {
        String compatibilityEvents = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionCompatibilityEvents.java"
        );

        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionBlackoutCooldownAdapter.java"
        )));
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionGunPunishmentAdapter.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutCooldownAdapter.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentAdapter.java"
        )));
        assertTrue(compatibilityEvents.contains(
                "import dev.caecorthus.sparkfactionapi.impl.blackout.FactionBlackoutCooldownAdapter;"
        ));
        assertTrue(compatibilityEvents.contains(
                "import dev.caecorthus.sparkfactionapi.impl.gun.FactionGunPunishmentAdapter;"
        ));
        assertTrue(compatibilityEvents.contains(
                "ShopPurchase.AFTER.register(FactionBlackoutCooldownAdapter::afterShopPurchase)"
        ));
        assertTrue(compatibilityEvents.contains(
                "ShouldPunishGunShooter.EVENT.register(FactionGunPunishmentAdapter::shouldPunishGunShooter)"
        ));
        assertFalse(compatibilityEvents.contains("PlayerShopComponentMixin"));
        assertFalse(compatibilityEvents.contains("PlayerMoodComponent"));
        assertFalse(compatibilityEvents.contains("WatheItems"));
    }

    @Test
    void packagedJarGuardRequiresClientVersionHandshakeClasses() throws IOException {
        String buildScript = readProjectFile("build.gradle");

        assertTrue(buildScript.contains("dev.caecorthus.sparkfactionapi.SparkFactionApiMod"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/client/SparkFactionApiClient.class"));
        assertTrue(buildScript.contains(
                "dev/caecorthus/sparkfactionapi/client/net/version/ClientVersionHandshake.class"
        ));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/net/version/ServerVersionHandshake.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/net/version/VersionProtocol.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentAdapter.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/impl/economy/FactionPassiveMoneyAdapter.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/mixin/MurderGameModePassiveMoneyMixin.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/mixin/GameRoundEndComponentMixin.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/client/mixin/RoundTextRendererMixin.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparkfactionapi/impl/FactionCapabilityBridge.class"));
        assertTrue(buildScript.contains("Forbidden retired class"));
        assertTrue(buildScript.contains("\"sparkfactionapi.mixins.json\""));
        assertTrue(buildScript.contains("\"sparkfactionapi.client.mixins.json\""));
    }

    @Test
    void versionHandshakeIsRegisteredByMainAndClientEntrypoints() throws IOException {
        String fabricMetadata = readProjectFile("src/main/resources/fabric.mod.json");
        String mainInitializer = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/SparkFactionApiMod.java"
        );
        String clientInitializer = readProjectFile(
                "src/client/java/dev/caecorthus/sparkfactionapi/client/SparkFactionApiClient.java"
        );

        assertTrue(fabricMetadata.contains("\"dev.caecorthus.sparkfactionapi.SparkFactionApiMod\""));
        assertTrue(fabricMetadata.contains("\"dev.caecorthus.sparkfactionapi.client.SparkFactionApiClient\""));
        assertTrue(mainInitializer.contains(
                "import dev.caecorthus.sparkfactionapi.net.version.ServerVersionHandshake;"
        ));
        assertTrue(mainInitializer.contains("ServerVersionHandshake.registerServer();"));
        assertTrue(clientInitializer.contains(
                "import dev.caecorthus.sparkfactionapi.client.net.version.ClientVersionHandshake;"
        ));
        assertTrue(clientInitializer.contains("ClientVersionHandshake.registerClient();"));
    }

    @Test
    void versionHandshakeUsesTargetPackages() {
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/net/SparkFactionVersionHandshake.java"
        )));
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/net/SparkFactionVersionCheck.java"
        )));
        assertFalse(Files.exists(Path.of(
                "src/client/java/dev/caecorthus/sparkfactionapi/client/net/SparkFactionClientVersionHandshake.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/net/version/VersionProtocol.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/net/version/ServerVersionHandshake.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/client/java/dev/caecorthus/sparkfactionapi/client/net/version/ClientVersionHandshake.java"
        )));
    }

    @Test
    void assignmentLogicUsesTargetPackage() throws IOException {
        String murderGameModeMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModeMixin.java"
        );

        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionAssignmentService.java"
        )));
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionSlotAllocator.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/assignment/FactionAssignmentService.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/assignment/FactionSlotAllocator.java"
        )));
        assertTrue(murderGameModeMixin.contains(
                "import dev.caecorthus.sparkfactionapi.impl.assignment.FactionAssignmentService;"
        ));
        assertFalse(murderGameModeMixin.contains("tickServerGameLoop"));
        assertFalse(murderGameModeMixin.contains("FactionPassiveMoneyAdapter"));
    }

    @Test
    void passiveMoneyLogicStaysOutsideMurderGameModeMixin() throws IOException {
        String mixinConfig = readProjectFile("src/main/resources/sparkfactionapi.mixins.json");
        String murderGameModeMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModeMixin.java"
        );
        Path passiveMoneyMixinPath = Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModePassiveMoneyMixin.java"
        );
        assertTrue(Files.exists(passiveMoneyMixinPath));
        String passiveMoneyMixin = Files.readString(passiveMoneyMixinPath);
        String passiveMoneyAdapter = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionPassiveMoneyAdapter.java"
        );

        assertTrue(mixinConfig.contains("\"MurderGameModeMixin\""));
        assertTrue(mixinConfig.contains("\"MurderGameModePassiveMoneyMixin\""));
        assertEquals(1, countOccurrences(mixinConfig, "\"MurderGameModeMixin\""));
        assertEquals(1, countOccurrences(mixinConfig, "\"MurderGameModePassiveMoneyMixin\""));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionPassiveMoneyAdapter.java"
        )));
        assertFalse(murderGameModeMixin.contains(
                "import dev.caecorthus.sparkfactionapi.impl.economy.FactionPassiveMoneyAdapter;"
        ));
        assertFalse(murderGameModeMixin.contains("FactionPassiveMoneyAdapter.applyCustomPassiveMoney"));
        assertFalse(murderGameModeMixin.contains("tickServerGameLoop"));
        assertFalse(murderGameModeMixin.contains("PlayerShopComponent"));
        assertFalse(murderGameModeMixin.contains("GameConstants"));
        assertFalse(murderGameModeMixin.contains("PASSIVE_MONEY_TICKER"));
        assertFalse(murderGameModeMixin.contains("KILLER_PASSIVE_MONEY_CAP"));
        assertFalse(murderGameModeMixin.contains("FactionEconomyRules.receivesCustomKillerPassiveMoney"));
        assertFalse(murderGameModeMixin.contains("FactionCapabilityBridge"));
        assertTrue(passiveMoneyMixin.contains(
                "import dev.caecorthus.sparkfactionapi.impl.economy.FactionPassiveMoneyAdapter;"
        ));
        assertTrue(passiveMoneyMixin.contains("method = \"tickServerGameLoop\""));
        assertTrue(passiveMoneyMixin.contains("at = @At(\"HEAD\")"));
        assertTrue(passiveMoneyMixin.contains("FactionPassiveMoneyAdapter.applyCustomPassiveMoney"));
        assertFalse(passiveMoneyMixin.contains("FactionAssignmentService"));
        assertFalse(passiveMoneyMixin.contains("FactionAssignmentPhase"));
        assertFalse(passiveMoneyMixin.contains("assignRolesAndGetKillerCount"));
        assertTrue(passiveMoneyAdapter.contains("GameWorldComponent.GameStatus.ACTIVE"));
        assertTrue(passiveMoneyAdapter.contains("GameConstants.PASSIVE_MONEY_TICKER"));
        assertTrue(passiveMoneyAdapter.contains("GameConstants.KILLER_PASSIVE_MONEY_CAP"));
        assertTrue(passiveMoneyAdapter.contains("FactionEconomyRules.receivesCustomKillerPassiveMoney"));
        assertTrue(passiveMoneyAdapter.contains("PlayerShopComponent.KEY.get"));
        assertTrue(passiveMoneyAdapter.contains("addToBalance(balanceToAdd)"));
    }

    @Test
    void roundEndTextRulesUseTargetPackage() throws IOException {
        String roundTextRendererMixin = readProjectFile(
                "src/client/java/dev/caecorthus/sparkfactionapi/client/mixin/RoundTextRendererMixin.java"
        );
        String gameFunctionsMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameFunctionsMixin.java"
        );

        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRoundEndTextRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/text/FactionRoundEndTextRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/text/FactionLetterTextRules.java"
        )));
        assertTrue(roundTextRendererMixin.contains(
                "import dev.caecorthus.sparkfactionapi.impl.text.FactionRoundEndTextRules;"
        ));
        assertTrue(roundTextRendererMixin.contains(
                "target = \"Ldev/doctor4t/wathe/client/gui/RoleAnnouncementTexts$RoleAnnouncementText;winText:Lnet/minecraft/text/Text;\""
        ));
        assertTrue(roundTextRendererMixin.contains(
                "target = \"Lnet/minecraft/util/Identifier;getPath()Ljava/lang/String;\""
        ));
        assertTrue(roundTextRendererMixin.contains(".orElse(roleText.winText)"));
        assertTrue(roundTextRendererMixin.contains(".orElseGet(roleId::getPath)"));
        assertTrue(roundTextRendererMixin.contains("!roundEnd.hasCustomWin()"));
        assertTrue(roundTextRendererMixin.contains("SparkFactionApi.getFaction(winningFaction)"));
        assertTrue(gameFunctionsMixin.contains(
                "import dev.caecorthus.sparkfactionapi.impl.text.FactionLetterTextRules;"
        ));
        assertTrue(gameFunctionsMixin.contains("FactionLetterTextRules.letterFactionName"));
        assertFalse(gameFunctionsMixin.contains("private static String letterFactionName("));
    }

    @Test
    void roundEndSemanticsUseRoundEndPackage() throws IOException {
        String bootstrap = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionRegistryBootstrap.java"
        );
        String roundEndMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameRoundEndComponentMixin.java"
        );
        String winService = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinService.java"
        );
        String winRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinRules.java"
        );
        String winnerCollector = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinnerCollector.java"
        );
        String roundEndRows = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionRoundEndRows.java"
        );
        String roundEndStateRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionRoundEndStateRules.java"
        );
        String roundEndComponent = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/component/SparkFactionRoundEndComponent.java"
        );

        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionWinService.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinService.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionWinnerCollector.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionRoundEndRows.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/roundend/FactionRoundEndStateRules.java"
        )));
        assertTrue(bootstrap.contains("import dev.caecorthus.sparkfactionapi.impl.roundend.FactionWinService;"));
        assertTrue(roundEndMixin.contains("import dev.caecorthus.sparkfactionapi.impl.roundend.FactionRoundEndRows;"));
        assertTrue(roundEndMixin.contains("import dev.caecorthus.sparkfactionapi.impl.roundend.FactionRoundEndStateRules;"));
        assertFalse(roundEndMixin.contains("new GameRoundEndComponent.RoundEndData"));
        assertFalse(roundEndMixin.contains("PlayerEndStatus."));
        assertTrue(roundEndMixin.contains("FactionRoundEndRows.rows"));
        assertTrue(roundEndMixin.contains("FactionRoundEndStateRules.shouldWriteCustomRows"));
        assertTrue(roundEndMixin.contains("FactionRoundEndStateRules.shouldClearBeforeWatheStatusWrite"));
        assertTrue(roundEndMixin.contains("FactionRoundEndStateRules.shouldClearBeforeWatheExplicitWinnerWrite"));
        assertTrue(roundEndMixin.contains(
                "method = \"setRoundEndData(Lnet/minecraft/server/world/ServerWorld;Ljava/util/Collection;)V\""
        ));
        assertTrue(roundEndMixin.contains("customRoundEnd.markCustomWinWritten()"));
        assertTrue(roundEndMixin.contains("@Inject(method = \"didWin\", at = @At(\"HEAD\"), cancellable = true)"));
        assertTrue(roundEndMixin.contains("if (customRoundEnd.hasCustomWin())"));
        assertTrue(roundEndMixin.contains("cir.setReturnValue(customRoundEnd.didWin(uuid))"));
        assertEquals(1, countOccurrences(
                readProjectFile("src/main/resources/sparkfactionapi.mixins.json"),
                "\"GameRoundEndComponentMixin\""
        ));
        assertTrue(winService.contains("ServerLifecycleEvents.SERVER_STARTING.register"));
        assertTrue(winService.contains("CheckWinCondition.EVENT.register(FactionWinService::checkWin)"));
        assertTrue(winService.contains("FactionWinRules.customWinResult"));
        assertTrue(winService.contains("roundEnd.setCustomWin"));
        assertTrue(winService.contains("CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.NEUTRAL)"));
        assertTrue(winRules.contains("return FactionWinResult.block()"));
        assertTrue(winnerCollector.contains("FactionRegistryImpl.resolveEffectiveFaction"));
        assertTrue(winnerCollector.contains("FactionRegistryImpl.resolveBaseFaction"));
        assertTrue(roundEndRows.contains("new GameRoundEndComponent.RoundEndData"));
        assertTrue(roundEndRows.contains("PlayerEndStatus.LEFT_DEAD"));
        assertTrue(roundEndStateRules.contains("winStatus == GameFunctions.WinStatus.NEUTRAL"));
        assertTrue(roundEndComponent.contains("private boolean pendingCustomWinWrite"));
        assertTrue(roundEndComponent.contains("this.pendingCustomWinWrite = true"));
        assertTrue(roundEndComponent.contains("this.pendingCustomWinWrite = false"));
    }

    @Test
    void registryStateUsesRegistryPackageModules() throws IOException {
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String bootstrap = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionRegistryBootstrap.java"
        );
        String catalog = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionCatalog.java"
        );
        String roleCatalog = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionRoleCatalog.java"
        );
        String resolvers = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/EffectiveFactionResolvers.java"
        );

        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionRegistryBootstrap.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionCatalog.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/FactionRoleCatalog.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/registry/EffectiveFactionResolvers.java"
        )));
        assertTrue(factionRegistry.contains("FactionRegistryBootstrap.bootstrap"));
        assertTrue(factionRegistry.contains("FactionCatalog.registerFaction"));
        assertTrue(factionRegistry.contains("FactionRoleCatalog.registerRole"));
        assertTrue(factionRegistry.contains("EffectiveFactionResolvers.resolve"));
        assertFalse(factionRegistry.contains("private static final Map<Identifier, FactionDefinition> FACTIONS"));
        assertFalse(factionRegistry.contains("private static final Map<Role, Identifier> ROLE_FACTIONS"));
        assertFalse(factionRegistry.contains("private static final Map<Role, Faction> ROLE_NATIVE_FACTIONS"));
        assertFalse(factionRegistry.contains("private static final List<EffectiveFactionResolver> EFFECTIVE_RESOLVERS"));
        assertFalse(factionRegistry.contains("private static boolean bootstrapped"));
        assertFalse(factionRegistry.contains("new Role("));
        assertFalse(factionRegistry.contains("WatheRoles.registerRole"));
        assertTrue(bootstrap.contains("FactionWinService.register()"));
        assertTrue(catalog.contains("private static final Map<Identifier, FactionDefinition> FACTIONS"));
        assertTrue(roleCatalog.contains("private static final Map<Role, Identifier> ROLE_FACTIONS"));
        assertTrue(roleCatalog.contains("private static final Map<Role, Faction> ROLE_NATIVE_FACTIONS"));
        assertTrue(resolvers.contains("private static final List<EffectiveFactionResolver> EFFECTIVE_RESOLVERS"));
    }

    @Test
    void capabilityLookupUsesCapabilityPackageWithoutPolicyOwnership() throws IOException {
        String lookup = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/capability/FactionCapabilityLookup.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/capability/FactionCapabilityLookup.java"
        )));
        assertTrue(lookup.contains("FactionRegistryImpl.resolveBaseFaction"));
        assertTrue(lookup.contains("FactionRegistryImpl.resolveEffectiveFaction"));
        assertTrue(lookup.contains("FactionRegistryImpl.capabilities"));
        assertTrue(lookup.contains("FactionRegistryImpl.isCustomFaction"));
        assertFalse(lookup.contains("FactionEconomyPolicy"));
        assertFalse(lookup.contains("FactionGunPunishmentPolicy"));
        assertFalse(lookup.contains("FactionBlackoutCooldownPolicy"));
        assertFalse(lookup.contains("FactionInstinctPolicy"));
        assertFalse(lookup.contains("FactionTargetEligibility"));

        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionEconomyRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionCohortRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionInstinctRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/target/FactionTargetRules.java"
        );
        assertNoPrivateCapabilityLookupHelpers(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/shop/FactionShopAccessRules.java"
        );
    }

    @Test
    void gunPunishmentRulesStayOutsideCapabilityBridge() throws IOException {
        String gunAdapter = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentAdapter.java"
        );
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String gunRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentRules.java"
        );
        String gunPolicies = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentPolicies.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/gun/FactionGunPunishmentPolicies.java"
        )));
        assertTrue(gunAdapter.contains("FactionGunPunishmentRules."));
        assertFalse(gunAdapter.contains("FactionCapabilityBridge"));
        assertTrue(gunAdapter.contains("shooter.getInventory().remove(stack -> stack.isOf(revolver), 1, shooter.getInventory())"));
        assertTrue(gunAdapter.contains("FactionGunPunishmentRules.consumesPunishableGunLikeKiller(shooter, gameComponent)"));
        assertTrue(gunAdapter.contains("shooter.dropItem(revolver.getDefaultStack(), false, false)"));
        assertTrue(gunAdapter.contains("droppedGun.setPickupDelay(10)"));
        assertTrue(gunAdapter.contains("droppedGun.setThrower(shooter)"));
        assertTrue(gunAdapter.contains("ServerPlayNetworking.send(shooter, new GunDropPayload())"));
        assertTrue(gunAdapter.contains("PlayerMoodComponent.KEY.get(shooter).setMood(0)"));
        assertTrue(gunAdapter.contains("gameComponent.addToPreventGunPickup(shooter)"));
        assertTrue(gunAdapter.contains("GameFunctions.killPlayer(shooter, true, null, GameConstants.DeathReasons.SHOT_INNOCENT)"));
        assertBefore(
                gunAdapter,
                "shooter.getInventory().remove(stack -> stack.isOf(revolver), 1, shooter.getInventory())",
                "FactionGunPunishmentRules.consumesPunishableGunLikeKiller(shooter, gameComponent)"
        );
        assertBefore(
                gunAdapter,
                "FactionGunPunishmentRules.consumesPunishableGunLikeKiller(shooter, gameComponent)",
                "shooter.dropItem(revolver.getDefaultStack(), false, false)"
        );
        assertBefore(
                gunAdapter,
                "shooter.dropItem(revolver.getDefaultStack(), false, false)",
                "ServerPlayNetworking.send(shooter, new GunDropPayload())"
        );
        assertTrue(factionRegistry.contains("FactionGunPunishmentPolicies.register"));
        assertTrue(factionRegistry.contains("FactionGunPunishmentPolicies.clearForTests"));
        assertFalse(factionRegistry.contains("GUN_PUNISHMENT_POLICIES"));
        assertTrue(gunRules.contains("FactionGunPunishmentPolicies.gunPunishmentPolicies()"));
        assertTrue(gunPolicies.contains("private static final List<FactionGunPunishmentPolicy> GUN_PUNISHMENT_POLICIES"));
    }

    @Test
    void economyRulesStayOutsideCapabilityBridge() throws IOException {
        String murderGameModeMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModeMixin.java"
        );
        String passiveMoneyMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/MurderGameModePassiveMoneyMixin.java"
        );
        String gameFunctionsMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/GameFunctionsMixin.java"
        );
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String economyRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionEconomyRules.java"
        );
        String economyPolicies = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionEconomyPolicies.java"
        );
        String passiveMoneyAdapter = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionPassiveMoneyAdapter.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionEconomyRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionEconomyPolicies.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionPassiveMoneyAdapter.java"
        )));
        assertFalse(murderGameModeMixin.contains("FactionPassiveMoneyAdapter.applyCustomPassiveMoney"));
        assertTrue(passiveMoneyMixin.contains("FactionPassiveMoneyAdapter.applyCustomPassiveMoney"));
        assertFalse(murderGameModeMixin.contains("FactionEconomyRules.receivesCustomKillerPassiveMoney"));
        assertFalse(passiveMoneyMixin.contains("FactionEconomyRules.receivesCustomKillerPassiveMoney"));
        assertTrue(passiveMoneyAdapter.contains("FactionEconomyRules.receivesCustomKillerPassiveMoney"));
        assertTrue(gameFunctionsMixin.contains("FactionKillRewardAdapter.applyCustomKillReward"));
        assertFalse(gameFunctionsMixin.contains("FactionEconomyRules.receivesCustomKillReward"));
        assertFalse(gameFunctionsMixin.contains("PlayerPoisonComponent"));
        assertFalse(gameFunctionsMixin.contains("PlayerShopComponent"));
        assertFalse(gameFunctionsMixin.contains("GameConstants.MONEY_PER_KILL"));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionKillRewardAdapter.java"
        )));
        assertTrue(readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/economy/FactionKillRewardAdapter.java"
        ).contains("FactionEconomyRules.receivesCustomKillReward"));
        assertTrue(factionRegistry.contains("FactionEconomyPolicies.register"));
        assertTrue(factionRegistry.contains("FactionEconomyPolicies.clearForTests"));
        assertFalse(factionRegistry.contains("ECONOMY_POLICIES"));
        assertTrue(economyRules.contains("FactionEconomyPolicies.economyPolicies()"));
        assertTrue(economyPolicies.contains("private static final List<FactionEconomyPolicy> ECONOMY_POLICIES"));
        assertTrue(economyRules.contains("FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)"));
        assertTrue(economyRules.contains("!gameComponent.canUseKillerFeatures(player)"));
        assertTrue(economyRules.contains("&& receivesKillerPassiveMoney(player, gameComponent)"));
        assertBefore(
                economyRules,
                "FactionCapabilityLookup.hasCustomEffectiveFaction(player, gameComponent)",
                "&& receivesKillerPassiveMoney(player, gameComponent)"
        );
    }

    @Test
    void blackoutRulesStayOutsideCapabilityBridge() throws IOException {
        String blackoutAdapter = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutCooldownAdapter.java"
        );
        String worldBlackoutMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/WorldBlackoutComponentMixin.java"
        );
        String publicFacade = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/api/SparkFactionApi.java"
        );
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String blackoutRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutRules.java"
        );
        String blackoutPolicies = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutCooldownPolicies.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/blackout/FactionBlackoutCooldownPolicies.java"
        )));
        assertTrue(blackoutAdapter.contains("FactionBlackoutRules.blackoutCooldownOverride"));
        assertTrue(blackoutAdapter.contains("FactionBlackoutRules.sharesCustomBlackoutCooldown"));
        assertFalse(blackoutAdapter.contains("FactionCapabilityBridge"));
        assertTrue(worldBlackoutMixin.contains("FactionBlackoutRules.hasBlackoutImmunity"));
        assertTrue(publicFacade.contains("FactionBlackoutRules.hasBlackoutImmunity"));
        assertTrue(factionRegistry.contains("FactionBlackoutCooldownPolicies.register"));
        assertTrue(factionRegistry.contains("FactionBlackoutCooldownPolicies.clearForTests"));
        assertFalse(factionRegistry.contains("BLACKOUT_COOLDOWN_POLICIES"));
        assertTrue(blackoutRules.contains("FactionBlackoutCooldownPolicies.blackoutCooldownPolicies()"));
        assertTrue(blackoutPolicies.contains(
                "private static final List<FactionBlackoutCooldownPolicy> BLACKOUT_COOLDOWN_POLICIES"
        ));
    }

    @Test
    void visionRulesStayOutsideCapabilityBridge() throws IOException {
        String clientInitializer = readProjectFile(
                "src/client/java/dev/caecorthus/sparkfactionapi/client/SparkFactionApiClient.java"
        );
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String instinctRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionInstinctRules.java"
        );
        String instinctPolicies = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionInstinctPolicies.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionCohortRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionInstinctRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/vision/FactionInstinctPolicies.java"
        )));
        assertTrue(clientInitializer.contains("FactionCohortRules.sharesCohort"));
        assertTrue(clientInitializer.contains("FactionInstinctRules.policyResult"));
        assertTrue(clientInitializer.contains("FactionInstinctRules.shouldUseCustomHighlight"));
        assertFalse(clientInitializer.contains("FactionCapabilityBridge"));
        assertTrue(factionRegistry.contains("FactionInstinctPolicies.register"));
        assertTrue(factionRegistry.contains("FactionInstinctPolicies.clearForTests"));
        assertFalse(factionRegistry.contains("INSTINCT_POLICIES"));
        assertTrue(instinctRules.contains("FactionInstinctPolicies.instinctPolicies()"));
        assertTrue(instinctPolicies.contains("private static final List<FactionInstinctPolicy> INSTINCT_POLICIES"));
    }

    @Test
    void targetAndShopRulesRetireCapabilityBridge() throws IOException {
        String factionRegistry = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionRegistryImpl.java"
        );
        String killerShopBuilderMixin = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/mixin/KillerShopBuilderMixin.java"
        );
        String targetRules = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/target/FactionTargetRules.java"
        );
        String targetPolicies = readProjectFile(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/target/FactionTargetPolicies.java"
        );

        assertCapabilityBridgeRetired();
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/shop/FactionShopAccessRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/target/FactionTargetRules.java"
        )));
        assertTrue(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/target/FactionTargetPolicies.java"
        )));
        assertTrue(factionRegistry.contains("FactionTargetRules.canTarget"));
        assertTrue(factionRegistry.contains("FactionTargetPolicies.register"));
        assertTrue(factionRegistry.contains("FactionTargetPolicies.clearForTests"));
        assertFalse(factionRegistry.contains("TARGET_ELIGIBILITY"));
        assertTrue(killerShopBuilderMixin.contains("FactionShopAccessRules.canUseKillerShop"));
        assertFalse(killerShopBuilderMixin.contains("FactionCapabilityBridge"));
        assertTrue(targetRules.contains("FactionTargetPolicies.targetEligibility()"));
        assertTrue(targetRules.contains("if (result != null)"));
        assertTrue(targetPolicies.contains("private static final List<FactionTargetEligibility> TARGET_ELIGIBILITY"));
    }

    private static void assertNoWathePredicateRedirect(String file) throws IOException {
        String source = readProjectFile(file);
        assertFalse(source.contains("@Redirect") && source.contains("canUseKillerFeatures"));
        assertFalse(source.contains("@Redirect") && source.contains("isInnocent"));
    }

    private static void assertNoPrivateCapabilityLookupHelpers(String file) throws IOException {
        String source = readProjectFile(file);
        assertTrue(source.contains("FactionCapabilityLookup"));
        assertFalse(source.contains("private static FactionCapabilities capabilities("));
        assertFalse(source.contains("private static boolean hasCustomEffectiveFaction("));
    }

    private static void assertCapabilityBridgeRetired() {
        assertFalse(Files.exists(Path.of(
                "src/main/java/dev/caecorthus/sparkfactionapi/impl/FactionCapabilityBridge.java"
        )));
    }

    private static String readProjectFile(String file) throws IOException {
        return Files.readString(Path.of(file));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static void assertBefore(String value, String first, String second) {
        int firstIndex = value.indexOf(first);
        int secondIndex = value.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing expected text: " + first);
        assertTrue(secondIndex >= 0, "Missing expected text: " + second);
        assertTrue(firstIndex < secondIndex, first + " should appear before " + second);
    }
}
