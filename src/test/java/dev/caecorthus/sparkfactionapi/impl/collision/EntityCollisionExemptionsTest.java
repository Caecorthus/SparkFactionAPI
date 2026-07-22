package dev.caecorthus.sparkfactionapi.impl.collision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityCollisionExemptionsTest {
    @Test
    void pairRuleIsSymmetric() {
        assertFalse(EntityCollisionExemptions.shouldCancelPush(false, false));
        assertTrue(EntityCollisionExemptions.shouldCancelPush(true, false));
        assertTrue(EntityCollisionExemptions.shouldCancelPush(false, true));
        assertTrue(EntityCollisionExemptions.shouldCancelPush(true, true));
    }

    @Test
    void registrationRejectsNullPredicates() {
        assertThrows(NullPointerException.class, () -> EntityCollisionExemptions.register(null));
    }

    @Test
    void nullAndUnregisteredEntitiesFailClosed() {
        assertFalse(EntityCollisionExemptions.isExempt(null));
        assertFalse(EntityCollisionExemptions.shouldCancelPush(null, null));
    }
}
