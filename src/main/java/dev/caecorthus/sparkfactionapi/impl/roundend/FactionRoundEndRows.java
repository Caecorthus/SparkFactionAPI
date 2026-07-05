package dev.caecorthus.sparkfactionapi.impl.roundend;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Builds Wathe round-end rows from custom faction round-end state.
 * 根据自定义阵营结算状态构造 Wathe 结算行。
 */
public final class FactionRoundEndRows {
    private FactionRoundEndRows() {
    }

    public static List<GameRoundEndComponent.RoundEndData> rows(
            ServerWorld serverWorld,
            GameWorldComponent game,
            SparkFactionRoundEndComponent customRoundEnd
    ) {
        return rows(
                game.getRoles(),
                game.getGameProfiles(),
                game::isPlayerDead,
                uuid -> serverWorld.getPlayerByUuid(uuid) != null,
                customRoundEnd::didWin
        );
    }

    public static @Nullable Identifier gameModeId(GameWorldComponent game) {
        GameMode gameMode = game.getGameMode();
        return gameMode != null ? gameMode.identifier : null;
    }

    static List<GameRoundEndComponent.RoundEndData> rows(
            Map<UUID, Role> roles,
            Map<UUID, GameProfile> profiles,
            Predicate<UUID> isDead,
            Predicate<UUID> isOnline,
            Predicate<UUID> didWin
    ) {
        List<GameRoundEndComponent.RoundEndData> rows = new ArrayList<>();
        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            GameProfile profile = profiles.get(uuid);
            if (profile == null || role == WatheRoles.NO_ROLE) {
                continue;
            }

            rows.add(new GameRoundEndComponent.RoundEndData(
                    profile,
                    role.identifier(),
                    endStatus(isDead.test(uuid), isOnline.test(uuid)),
                    didWin.test(uuid)
            ));
        }
        return rows;
    }

    static GameRoundEndComponent.PlayerEndStatus endStatus(boolean wasDead, boolean isOnline) {
        if (wasDead) {
            return isOnline
                    ? GameRoundEndComponent.PlayerEndStatus.DEAD
                    : GameRoundEndComponent.PlayerEndStatus.LEFT_DEAD;
        }
        return isOnline
                ? GameRoundEndComponent.PlayerEndStatus.ALIVE
                : GameRoundEndComponent.PlayerEndStatus.LEFT;
    }
}
