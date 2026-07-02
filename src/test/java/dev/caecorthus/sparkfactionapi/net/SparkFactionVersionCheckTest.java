package dev.caecorthus.sparkfactionapi.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SparkFactionVersionCheckTest {
    @Test
    void matchingVersionsAreCompatible() {
        assertTrue(SparkFactionVersionCheck.isCompatible("0.1.6.1", "0.1.6.1"));
    }

    @Test
    void differentOrBlankVersionsAreRejected() {
        assertFalse(SparkFactionVersionCheck.isCompatible("0.1.6.1", "0.1.6"));
        assertFalse(SparkFactionVersionCheck.isCompatible("0.1.6.1", ""));
        assertFalse(SparkFactionVersionCheck.isCompatible("0.1.6.1", null));
    }

    @Test
    void disconnectMessagesNameExpectedAndActualVersions() {
        assertEquals(
                "SparkFactionAPI is required on the client with version 0.1.6.1.",
                SparkFactionVersionCheck.missingClientMessage("0.1.6.1")
        );
        assertEquals(
                "SparkFactionAPI version mismatch: server=0.1.6.1, client=0.1.6. "
                        + "Please install the same SparkFactionAPI version as the server.",
                SparkFactionVersionCheck.mismatchMessage("0.1.6.1", "0.1.6")
        );
    }
}
