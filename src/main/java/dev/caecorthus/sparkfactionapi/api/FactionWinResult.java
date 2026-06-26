package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record FactionWinResult(Type type, @Nullable Identifier winningFaction) {
    public enum Type {
        NONE,
        BLOCK,
        FACTION_WIN
    }

    private static final FactionWinResult NONE = new FactionWinResult(Type.NONE, null);
    private static final FactionWinResult BLOCK = new FactionWinResult(Type.BLOCK, null);

    public static FactionWinResult none() {
        return NONE;
    }

    public static FactionWinResult block() {
        return BLOCK;
    }

    public static FactionWinResult factionWin(Identifier factionId) {
        return new FactionWinResult(Type.FACTION_WIN, factionId);
    }
}
