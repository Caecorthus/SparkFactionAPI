package dev.caecorthus.sparkfactionapi.net.version;

import org.junit.jupiter.api.Test;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionProtocolTest {
    @Test
    void versionCheckChannelIdStaysStable() {
        assertEquals(Identifier.of("sparkfactionapi", "version_check"), VersionProtocol.VERSION_CHECK_ID);
    }

    @Test
    void versionPacketRoundTripsExactFriendlyVersion() {
        PacketByteBuf packet = VersionProtocol.writeVersion("0.1.5.3");

        assertEquals("0.1.5.3", VersionProtocol.readVersion(packet));
        assertEquals(0, packet.readableBytes());
    }

    @Test
    void matchingVersionsAreCompatible() {
        assertTrue(VersionProtocol.isCompatible("0.1.6.1", "0.1.6.1"));
    }

    @Test
    void differentOrBlankVersionsAreRejected() {
        assertFalse(VersionProtocol.isCompatible("0.1.6.1", "0.1.6"));
        assertFalse(VersionProtocol.isCompatible("0.1.6.1", ""));
        assertFalse(VersionProtocol.isCompatible("0.1.6.1", null));
    }

    @Test
    void unansweredLoginQueriesAreAllowedForProxyTransfers() {
        assertFalse(VersionProtocol.shouldRejectUnansweredLoginQuery());
    }

    @Test
    void disconnectMessagesNameExpectedAndActualVersions() {
        assertEquals(
                "SparkFactionAPI is required on the client with version 0.1.6.1.",
                VersionProtocol.missingClientMessage("0.1.6.1")
        );
        assertEquals(
                "SparkFactionAPI version mismatch: server=0.1.6.1, client=0.1.6. "
                        + "Please install the same SparkFactionAPI version as the server.",
                VersionProtocol.mismatchMessage("0.1.6.1", "0.1.6")
        );
    }
}
