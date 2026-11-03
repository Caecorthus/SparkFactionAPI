package dev.caecorthus.sparkfactionapi.client.team;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.UUID;

/**
 * Finds the player a name on screen belongs to, by account name: first among players online, then in Wathe's cache
 * of everyone seen this session (disguises can imitate players who have left).
 * 按账号名查屏幕上的名字是哪位玩家：先查在线玩家，再查 Wathe 缓存的本次见过的所有玩家（伪装对象可能已经下线）。
 */
public final class ShownPlayerNames {
    private ShownPlayerNames() {
    }

    public static UUID uuidOf(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        PlayerListEntry online = handler == null ? null : handler.getPlayerListEntry(name);
        if (online != null) {
            return online.getProfile().getId();
        }
        for (PlayerListEntry cached : WatheClient.PLAYER_ENTRIES_CACHE.values()) {
            if (name.equals(cached.getProfile().getName())) {
                return cached.getProfile().getId();
            }
        }
        return null;
    }
}
