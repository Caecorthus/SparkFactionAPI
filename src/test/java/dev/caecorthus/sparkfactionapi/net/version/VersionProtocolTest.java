package dev.caecorthus.sparkfactionapi.net.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class VersionProtocolTest {
    @Test
    void versionCheckChannelIdStaysStable() {
        assertEquals(Identifier.of("sparkfactionapi", "version_check"), VersionProtocol.VERSION_CHECK_ID);
    }

    @Test
    void versionPacketRoundTripsExactFriendlyVersion() {
        PacketByteBuf packet = VersionProtocol.writeVersion("0.1.5.6");

        assertEquals("0.1.5.6", VersionProtocol.readVersion(packet));
        assertEquals(0, packet.readableBytes());
    }

    @Test
    void compatibilityRequiresMatchingNonBlankVersions() {
        assertTrue(VersionProtocol.isCompatible("0.1.5.6", "0.1.5.6"));
        assertFalse(VersionProtocol.isCompatible("0.1.5.6", "0.1.5.5"));
        assertFalse(VersionProtocol.isCompatible("0.1.5.6", ""));
        assertFalse(VersionProtocol.isCompatible("0.1.5.6", null));
    }

    @Test
    void unansweredLoginQueriesRemainAllowedForProxyTransfers() {
        assertFalse(VersionProtocol.shouldRejectUnansweredLoginQuery());
    }
}
