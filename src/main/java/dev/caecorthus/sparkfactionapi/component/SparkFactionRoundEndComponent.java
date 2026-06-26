package dev.caecorthus.sparkfactionapi.component;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores custom faction round-end state without extending Wathe's WinStatus enum.
 * 保存自定义阵营结算状态，不扩展 Wathe 固定的 WinStatus 枚举。
 */
public final class SparkFactionRoundEndComponent implements AutoSyncedComponent {
    public static final ComponentKey<SparkFactionRoundEndComponent> KEY =
            ComponentRegistry.getOrCreate(SparkFactionApiMod.id("round_end"), SparkFactionRoundEndComponent.class);

    private final Scoreboard scoreboard;
    private @Nullable Identifier winningFaction;
    private final LinkedHashSet<UUID> winners = new LinkedHashSet<>();

    public SparkFactionRoundEndComponent(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public SparkFactionRoundEndComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this(scoreboard);
    }

    public void clearCustomWin() {
        this.winningFaction = null;
        this.winners.clear();
        sync();
    }

    public void setCustomWin(Identifier winningFaction, Set<UUID> winners) {
        this.winningFaction = winningFaction;
        this.winners.clear();
        this.winners.addAll(winners);
        sync();
    }

    public boolean hasCustomWin() {
        return winningFaction != null;
    }

    public @Nullable Identifier getWinningFaction() {
        return winningFaction;
    }

    public Set<UUID> getWinners() {
        return Set.copyOf(winners);
    }

    public boolean didWin(UUID uuid) {
        return winners.contains(uuid);
    }

    public void sync() {
        KEY.sync(scoreboard);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("WinningFaction", NbtElement.STRING_TYPE)) {
            this.winningFaction = Identifier.of(tag.getString("WinningFaction"));
        } else {
            this.winningFaction = null;
        }

        this.winners.clear();
        if (tag.contains("Winners", NbtElement.LIST_TYPE)) {
            for (NbtElement element : tag.getList("Winners", NbtElement.INT_ARRAY_TYPE)) {
                this.winners.add(NbtHelper.toUuid(element));
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (winningFaction != null) {
            tag.putString("WinningFaction", winningFaction.toString());
        }

        NbtList winnerList = new NbtList();
        for (UUID winner : winners) {
            winnerList.add(NbtHelper.fromUuid(winner));
        }
        tag.put("Winners", winnerList);
    }
}
