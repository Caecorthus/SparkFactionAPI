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
import java.util.Objects;
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
    // Server-only handoff guard between FactionWinService and Wathe's next round-end write.
    // 服务端临时交接标记，用于连接 FactionWinService 与 Wathe 随后的结算写入，不持久化。
    private boolean pendingCustomWinWrite;
    private final LinkedHashSet<UUID> winners = new LinkedHashSet<>();

    public SparkFactionRoundEndComponent(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public SparkFactionRoundEndComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this(scoreboard);
    }

    // Wathe's CheckWinCondition reaches these every round tick, so both sync only when the synced
    // faction or winner set changes; the server-only pending flag is updated on every call.
    // Wathe 的 CheckWinCondition 每个对局 tick 都会调用这两个方法，因此仅在同步的阵营或胜者集合变化时同步；
    // 仅服务端使用的待写入标记每次调用都会更新。
    public void clearCustomWin() {
        boolean visibleChange = VisibleState.changes(this.winningFaction, this.winners, null, Set.of());
        this.winningFaction = null;
        this.pendingCustomWinWrite = false;
        this.winners.clear();
        if (visibleChange) {
            sync();
        }
    }

    public void setCustomWin(Identifier winningFaction, Set<UUID> winners) {
        boolean visibleChange = VisibleState.changes(this.winningFaction, this.winners, winningFaction, winners);
        this.winningFaction = winningFaction;
        this.pendingCustomWinWrite = true;
        this.winners.clear();
        this.winners.addAll(winners);
        if (visibleChange) {
            sync();
        }
    }

    /**
     * Pure client-visible change check, nested so calling it never initializes {@link #KEY}.
     * Winners compare as a set: clients only read membership, never order.
     * 纯函数的客户端可见变化判断；放在嵌套类中，调用时不会初始化 {@link #KEY}。
     * 胜者按集合比较：客户端只读取成员关系，不依赖顺序。
     */
    static final class VisibleState {
        private VisibleState() {
        }

        static boolean changes(
                @Nullable Identifier currentFaction,
                Set<UUID> currentWinners,
                @Nullable Identifier nextFaction,
                Set<UUID> nextWinners
        ) {
            return !Objects.equals(currentFaction, nextFaction) || !currentWinners.equals(nextWinners);
        }
    }

    public boolean hasCustomWin() {
        return winningFaction != null;
    }

    public boolean hasPendingCustomWinWrite() {
        return pendingCustomWinWrite;
    }

    public void markCustomWinWritten() {
        this.pendingCustomWinWrite = false;
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
        this.pendingCustomWinWrite = false;

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
