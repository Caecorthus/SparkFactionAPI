package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionRoleDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.target.FactionTargetRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionTargetRulesTest {
    @BeforeEach
    void setUp() {
        FactionRegistryImpl.clearForTests();
        SparkFactionApi.bootstrap();
    }

    @AfterEach
    void tearDown() {
        FactionRegistryImpl.clearForTests();
    }

    @Test
    void targetTagsGateBaseTargetEligibility() {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");
        Identifier viewerFaction = Identifier.of("sparkwitch", "hex_viewer");
        Identifier targetFaction = Identifier.of("sparkwitch", "hex_target_pool");
        SparkFactionApi.registerFaction(FactionDefinition.builder(viewerFaction).build());
        SparkFactionApi.registerFaction(FactionDefinition.builder(targetFaction)
                .capabilities(FactionCapabilities.builder()
                        .targetTag(targetTag)
                        .build())
                .build());
        Role viewer = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "hex_viewer_role"),
                        viewerFaction
                )
                .build());
        Role target = SparkFactionApi.registerRole(FactionRoleDefinition.builder(
                        Identifier.of("sparkwitch", "hex_target_role"),
                        targetFaction
                )
                .build());

        assertTrue(FactionTargetRules.canTarget(viewer, target, targetTag));
        assertFalse(FactionTargetRules.canTarget(target, viewer, targetTag));
    }

    @Test
    void missingTargetTagCannotMatch() {
        assertFalse(FactionTargetRules.canTarget(null, null, null));
    }

    @Test
    void publicPlayerTargetQueryRejectsMissingContext() {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");

        assertFalse(SparkFactionApi.canTarget(null, null, targetTag, null));
    }

    @Test
    void targetEligibilityPoliciesUseFirstNonNullOverride() throws Exception {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");
        SparkFactionApi.registerTargetEligibility((viewer, target, tag, gameComponent) -> null);
        SparkFactionApi.registerTargetEligibility((viewer, target, tag, gameComponent) -> Boolean.TRUE);
        SparkFactionApi.registerTargetEligibility((viewer, target, tag, gameComponent) -> Boolean.FALSE);

        assertEquals(Boolean.TRUE, invokeTargetEligibilityResult(targetTag));
    }

    @Test
    void targetEligibilityPoliciesCanReturnFalseOverride() throws Exception {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");
        SparkFactionApi.registerTargetEligibility((viewer, target, tag, gameComponent) -> Boolean.FALSE);

        assertEquals(Boolean.FALSE, invokeTargetEligibilityResult(targetTag));
    }

    @Test
    void registryTestResetClearsTargetPolicies() throws Exception {
        Identifier targetTag = Identifier.of("sparkwitch", "hex_target");
        SparkFactionApi.registerTargetEligibility((viewer, target, tag, gameComponent) -> Boolean.TRUE);

        assertEquals(Boolean.TRUE, invokeTargetEligibilityResult(targetTag));

        FactionRegistryImpl.clearForTests();

        assertNull(invokeTargetEligibilityResult(targetTag));
    }

    private static Boolean invokeTargetEligibilityResult(Identifier targetTag)
            throws ReflectiveOperationException {
        Method method = FactionTargetRules.class.getDeclaredMethod(
                "firstTargetEligibilityResult",
                PlayerEntity.class,
                PlayerEntity.class,
                Identifier.class,
                GameWorldComponent.class
        );
        method.setAccessible(true);
        try {
            return (Boolean) method.invoke(null, null, null, targetTag, null);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }
}
