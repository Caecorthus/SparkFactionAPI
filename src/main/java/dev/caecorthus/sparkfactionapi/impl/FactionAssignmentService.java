package dev.caecorthus.sparkfactionapi.impl;

import dev.caecorthus.sparkfactionapi.api.FactionAssignmentContext;
import dev.caecorthus.sparkfactionapi.api.FactionAssignmentPhase;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FactionAssignmentService {
    private FactionAssignmentService() {
    }

    public static int assignPhase(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            FactionAssignmentPhase phase
    ) {
        List<ServerPlayerEntity> availablePlayers = availablePlayers(gameComponent, players);
        if (availablePlayers.isEmpty()) {
            return 0;
        }

        List<AssignmentRequest> requests = requestsForPhase(world, gameComponent, players, availablePlayers.size(), phase);
        if (requests.isEmpty()) {
            return 0;
        }

        shuffle(availablePlayers, world.getRandom());
        int totalDesired = requests.stream().mapToInt(AssignmentRequest::remainingSlots).sum();
        List<AssignmentRequest> assignmentOrder = totalDesired > availablePlayers.size()
                ? fairShortageOrder(requests, availablePlayers.size(), world)
                : priorityOrder(requests);

        int assigned = 0;
        int playerIndex = 0;
        RoleSelectionContext roleContext = roleSelectionContext(world, gameComponent, players);
        for (AssignmentRequest request : assignmentOrder) {
            while (request.remainingSlots() > 0 && playerIndex < availablePlayers.size()) {
                Role role = request.takeRole(roleContext, gameComponent, world);
                if (role == null) {
                    break;
                }
                gameComponent.addRole(availablePlayers.get(playerIndex++), role);
                request.consumeSlot();
                assigned++;
            }
        }
        return assigned;
    }

    private static List<ServerPlayerEntity> availablePlayers(GameWorldComponent gameComponent, List<ServerPlayerEntity> players) {
        List<ServerPlayerEntity> available = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role == null || role == WatheRoles.NO_ROLE) {
                available.add(player);
            }
        }
        return available;
    }

    private static List<AssignmentRequest> requestsForPhase(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            int availablePlayerCount,
            FactionAssignmentPhase phase
    ) {
        FactionAssignmentContext context = new FactionAssignmentContext(
                world,
                gameComponent,
                List.copyOf(players),
                players.size(),
                availablePlayerCount,
                phase
        );
        List<AssignmentRequest> requests = new ArrayList<>();
        RoleSelectionContext roleContext = roleSelectionContext(world, gameComponent, players);
        for (FactionDefinition faction : FactionRegistryImpl.getCustomFactions()) {
            if (faction.assignmentPhase() != phase) {
                continue;
            }
            int desired = Math.max(0, faction.assignmentPolicy().desiredSlots(context));
            List<Role> roles = eligibleRoles(faction, roleContext, gameComponent);
            int slots = Math.min(desired, roles.size());
            if (slots > 0) {
                requests.add(new AssignmentRequest(faction, roles, slots));
            }
        }
        return requests;
    }

    private static List<Role> eligibleRoles(
            FactionDefinition faction,
            RoleSelectionContext roleContext,
            GameWorldComponent gameComponent
    ) {
        List<Role> roles = new ArrayList<>();
        for (Role role : FactionRegistryImpl.getRolesForFaction(faction.id())) {
            if (gameComponent.isRoleEnabled(role) && role.shouldAppear(roleContext)) {
                roles.add(role);
            }
        }
        return roles;
    }

    private static List<AssignmentRequest> priorityOrder(List<AssignmentRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparingInt((AssignmentRequest request) -> request.faction().priority()).reversed())
                .toList();
    }

    /**
     * Shortage mode ignores priority and gives each competing faction a fair pass.
     * 名额短缺时忽略 priority，让每个竞争阵营公平轮抽。
     */
    private static List<AssignmentRequest> fairShortageOrder(
            List<AssignmentRequest> requests,
            int availableSlots,
            ServerWorld world
    ) {
        List<Integer> desired = requests.stream().map(AssignmentRequest::remainingSlots).toList();
        List<Integer> grantedSlots = FactionSlotAllocator.allocateShortage(
                desired,
                availableSlots,
                new java.util.Random(world.getRandom().nextLong())
        );
        List<AssignmentRequest> result = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            int slots = grantedSlots.get(index);
            if (slots > 0) {
                result.add(requests.get(index).copyWithSlots(slots));
            }
        }
        return result;
    }

    private static RoleSelectionContext roleSelectionContext(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players
    ) {
        int totalPlayerCount = players.size();
        int targetKillerCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getKillerDividend());
        int targetNeutralCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getNeutralDividend());
        int targetVigilanteCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getVigilanteDividend());
        return new RoleSelectionContext(
                world,
                gameComponent,
                List.copyOf(players),
                totalPlayerCount,
                targetKillerCount,
                targetNeutralCount,
                targetVigilanteCount
        );
    }

    private static final class AssignmentRequest {
        private final FactionDefinition faction;
        private final List<Role> roles;
        private int remainingSlots;

        private AssignmentRequest(FactionDefinition faction, List<Role> roles, int remainingSlots) {
            this.faction = faction;
            this.roles = new ArrayList<>(roles);
            this.remainingSlots = remainingSlots;
        }

        private FactionDefinition faction() {
            return faction;
        }

        private int remainingSlots() {
            return remainingSlots;
        }

        private void consumeSlot() {
            remainingSlots--;
        }

        private AssignmentRequest copyWithSlots(int slots) {
            return new AssignmentRequest(faction, roles, slots);
        }

        private Role takeRole(RoleSelectionContext context, GameWorldComponent gameComponent, ServerWorld world) {
            shuffle(roles, world.getRandom());
            for (int index = 0; index < roles.size(); index++) {
                Role role = roles.get(index);
                if (gameComponent.isRoleEnabled(role) && role.shouldAppear(context)) {
                    roles.remove(index);
                    return role;
                }
            }
            return null;
        }
    }

    private static <T> void shuffle(List<T> values, Random random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(swap));
            values.set(swap, value);
        }
    }
}
