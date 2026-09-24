package dev.caecorthus.sparkfactionapi.api;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliceRolesTest {
    @Test
    void nativeVigilanteAndVeteranAreDefaultsWithoutOwnerRegistration() {
        for (String path : List.of("vigilante", "veteran")) {
            Identifier id = Identifier.of("wathe", path);
            assertTrue(PoliceRoles.contains(id));
            assertTrue(PoliceRoles.contains(role(id, true, false)));
        }
    }

    @Test
    void nullUnknownAndUnrelatedRolesAreNotPolice() {
        assertFalse(PoliceRoles.contains((Identifier) null));
        assertFalse(PoliceRoles.contains((Role) null));
        for (String value : List.of(
                "wathe:civilian", "wathe:killer", "wathe:none", "wathe:neutral",
                "sparkwitch:judge", "sparkwitch:grandwitch", "noellesroles:sheriff",
                "police_test:vigilante", "police_test:veteran", "police_test:unknown"
        )) {
            Identifier id = Identifier.of(value);
            assertFalse(PoliceRoles.contains(id), value);
            assertFalse(PoliceRoles.contains(role(id, true, false)), value);
        }
    }

    @Test
    void nullRegistrationIsRejectedWithoutDisturbingDefaults() {
        assertThrows(NullPointerException.class, () -> PoliceRoles.register(null));
        assertTrue(PoliceRoles.contains(Identifier.of("wathe", "vigilante")));
        assertTrue(PoliceRoles.contains(Identifier.of("wathe", "veteran")));
    }

    @Test
    void ownersCanRegisterIdentifiersBeforeRolesExist() {
        Identifier id = Identifier.of("police_test", "custom_owner");
        PoliceRoles.register(id);

        Identifier equivalentId = Identifier.of("police_test:custom_owner");
        assertTrue(PoliceRoles.contains(equivalentId));
        assertTrue(PoliceRoles.contains(role(equivalentId, true, false)));
        assertFalse(PoliceRoles.contains(Identifier.of("another_owner", "custom_owner")));
    }

    @Test
    void registrationIsIdempotentForNativeAndCustomIdentifiers() {
        Identifier custom = Identifier.of("police_test", "repeated");
        Identifier nativeId = Identifier.of("wathe", "veteran");
        for (int attempt = 0; attempt < 10; attempt++) {
            PoliceRoles.register(custom);
            PoliceRoles.register(nativeId);
        }

        assertTrue(PoliceRoles.contains(custom));
        assertTrue(PoliceRoles.contains(nativeId));
        assertTrue(PoliceRoles.contains(Identifier.of("wathe", "vigilante")));
    }

    @Test
    void concurrentOwnerRegistrationPreservesAllMemberships() throws Exception {
        Identifier shared = Identifier.of("police_test", "concurrent_shared");
        List<Identifier> ids = new ArrayList<>();
        List<Callable<Void>> registrations = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            Identifier id = Identifier.of("police_test", "concurrent_" + index);
            ids.add(id);
            registrations.add(() -> {
                PoliceRoles.register(shared);
                PoliceRoles.register(id);
                assertTrue(PoliceRoles.contains(shared));
                assertTrue(PoliceRoles.contains(id));
                return null;
            });
        }

        try (var executor = Executors.newFixedThreadPool(4)) {
            for (var result : executor.invokeAll(registrations, 10, TimeUnit.SECONDS)) {
                result.get();
            }
        }
        assertTrue(PoliceRoles.contains(shared));
        for (Identifier id : ids) {
            assertTrue(PoliceRoles.contains(id));
        }
    }

    @Test
    void classificationNeitherDependsOnNorChangesFactionOrAllocationProperties() {
        Identifier id = Identifier.of("police_test", "faction_independent");
        List<Role> sameIdentifierRoles = List.of(
                role(id, true, false),
                role(id, false, true),
                role(id, false, false)
        );
        var factionsBefore = sameIdentifierRoles.stream().map(Role::getFaction).toList();
        for (Role role : sameIdentifierRoles) {
            role.setSlotCost(3);
            role.setSpawnGroupSize(2);
            assertFalse(PoliceRoles.contains(role));
        }

        PoliceRoles.register(id);

        assertEquals(factionsBefore, sameIdentifierRoles.stream().map(Role::getFaction).toList());
        for (Role role : sameIdentifierRoles) {
            assertTrue(PoliceRoles.contains(role));
            assertEquals(3, role.getSlotCost());
            assertEquals(2, role.getSpawnGroupSize());
        }
        assertFalse(PoliceRoles.contains(role(Identifier.of("police_test", "other_civilian"), true, false)));
        assertFalse(PoliceRoles.contains(role(Identifier.of("police_test", "other_killer"), false, true)));
        assertFalse(PoliceRoles.contains(role(Identifier.of("police_test", "other_neutral"), false, false)));
    }

    private static Role role(Identifier id, boolean innocent, boolean killer) {
        return new Role(id, 0xFFFFFF, innocent, killer, Role.MoodType.NONE, 0, false);
    }
}
