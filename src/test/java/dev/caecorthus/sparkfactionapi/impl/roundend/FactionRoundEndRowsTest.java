package dev.caecorthus.sparkfactionapi.impl.roundend;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionRoundEndRowsTest {
    @Test
    void endStatusPreservesWatheAliveDeadAndLeftStates() {
        assertEquals(
                GameRoundEndComponent.PlayerEndStatus.ALIVE,
                FactionRoundEndRows.endStatus(false, true)
        );
        assertEquals(
                GameRoundEndComponent.PlayerEndStatus.LEFT,
                FactionRoundEndRows.endStatus(false, false)
        );
        assertEquals(
                GameRoundEndComponent.PlayerEndStatus.DEAD,
                FactionRoundEndRows.endStatus(true, true)
        );
        assertEquals(
                GameRoundEndComponent.PlayerEndStatus.LEFT_DEAD,
                FactionRoundEndRows.endStatus(true, false)
        );
    }

    @Test
    void rowsSkipMissingProfilesAndNoRoleEntries() {
        UUID winner = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID missingProfile = UUID.fromString("00000000-0000-0000-0000-000000000202");
        UUID noRole = UUID.fromString("00000000-0000-0000-0000-000000000203");
        Map<UUID, dev.doctor4t.wathe.api.Role> roles = new LinkedHashMap<>();
        roles.put(winner, WatheRoles.CIVILIAN);
        roles.put(missingProfile, WatheRoles.CIVILIAN);
        roles.put(noRole, WatheRoles.NO_ROLE);
        Map<UUID, GameProfile> profiles = new LinkedHashMap<>();
        profiles.put(winner, new GameProfile(winner, "Winner"));
        profiles.put(noRole, new GameProfile(noRole, "NoRole"));

        var rows = FactionRoundEndRows.rows(
                roles,
                profiles,
                uuid -> false,
                uuid -> true,
                Set.of(winner)::contains
        );

        assertEquals(1, rows.size());
        assertEquals(WatheRoles.CIVILIAN.identifier(), rows.getFirst().role());
        assertEquals(GameRoundEndComponent.PlayerEndStatus.ALIVE, rows.getFirst().endStatus());
        assertEquals(true, rows.getFirst().isWinner());
    }
}
