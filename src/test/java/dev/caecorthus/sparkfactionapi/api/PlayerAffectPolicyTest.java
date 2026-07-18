package dev.caecorthus.sparkfactionapi.api;

import dev.caecorthus.sparkfactionapi.impl.target.PlayerAffectRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAffectPolicyTest {
    private static final Identifier ACTION_ID = Identifier.of("sparkfactionapi", "test_action");

    @Test
    void publicApiExposesExactPlayerAffectContract() throws NoSuchMethodException {
        Method query = SparkFactionApi.class.getDeclaredMethod(
                "canAffectPlayer",
                PlayerEntity.class,
                PlayerEntity.class,
                Identifier.class,
                GameWorldComponent.class
        );
        Method register = SparkFactionApi.class.getDeclaredMethod(
                "registerPlayerAffectPolicy",
                PlayerAffectPolicy.class
        );

        assertEquals(boolean.class, query.getReturnType());
        assertEquals(void.class, register.getReturnType());
    }

    @Test
    void noPoliciesAllowsTheInteraction() {
        assertTrue(PlayerAffectRules.canAffectPlayer(
                List.of(), null, null, ACTION_ID, null
        ));
    }

    @Test
    void everyPolicyRunsEvenAfterAVeto() {
        AtomicInteger calls = new AtomicInteger();
        List<PlayerAffectPolicy> policies = List.of(
                policy(calls, true),
                policy(calls, false),
                policy(calls, true)
        );

        assertFalse(PlayerAffectRules.canAffectPlayer(
                policies, null, null, ACTION_ID, null
        ));
        assertEquals(3, calls.get());
    }

    @Test
    void oneFalseResultVetoesOtherwiseAllowedPolicies() {
        List<PlayerAffectPolicy> policies = List.of(
                (actor, target, actionId, gameComponent) -> true,
                (actor, target, actionId, gameComponent) -> false,
                (actor, target, actionId, gameComponent) -> true
        );

        assertFalse(PlayerAffectRules.canAffectPlayer(
                policies, null, null, ACTION_ID, null
        ));
    }

    @Test
    void registrationOrderCannotRestoreAnAllowedResult() {
        PlayerAffectPolicy allow = (actor, target, actionId, gameComponent) -> true;
        PlayerAffectPolicy deny = (actor, target, actionId, gameComponent) -> false;

        assertFalse(PlayerAffectRules.canAffectPlayer(
                List.of(deny, allow), null, null, ACTION_ID, null
        ));
        assertFalse(PlayerAffectRules.canAffectPlayer(
                List.of(allow, deny), null, null, ACTION_ID, null
        ));
    }

    private static PlayerAffectPolicy policy(AtomicInteger calls, boolean result) {
        return (actor, target, actionId, gameComponent) -> {
            calls.incrementAndGet();
            return result;
        };
    }
}
