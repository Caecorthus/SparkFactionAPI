package dev.caecorthus.sparkfactionapi.api;

import net.minecraft.util.Identifier;

public final class FactionIds {
    public static final Identifier NONE = Identifier.of("wathe", "none");
    public static final Identifier CIVILIAN = Identifier.of("wathe", "civilian");
    public static final Identifier KILLER = Identifier.of("wathe", "killer");
    public static final Identifier NEUTRAL = Identifier.of("wathe", "neutral");

    private FactionIds() {
    }
}
