package dev.caecorthus.sparkfactionapi.component;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapResetRules;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * Server-wide progress of the lobby map reset, synced so clients can draw it and hold Wathe's auto-start countdown.
 * Runtime state only: a reset never survives a restart, so nothing is saved.
 * 全服大厅地图重置的进度，同步给客户端用于绘制进度并让 Wathe 自动开局倒计时停住。
 * 仅运行时状态：重置不会跨越重启，因此不持久化。
 */
public final class SparkFactionMapResetComponent implements AutoSyncedComponent {
    public static final ComponentKey<SparkFactionMapResetComponent> KEY = ComponentRegistry.getOrCreate(
            SparkFactionApiMod.id("map_reset"),
            SparkFactionMapResetComponent.class
    );

    private final Scoreboard scoreboard;
    private int progress = LobbyMapResetRules.INACTIVE;

    public SparkFactionMapResetComponent(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public SparkFactionMapResetComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this(scoreboard);
    }

    /** -1 none, 0-99 resetting, 100 just completed. / -1 无，0-99 重置中，100 刚完成。 */
    public int getProgress() {
        return progress;
    }

    public boolean isResetting() {
        return LobbyMapResetRules.isResetting(progress);
    }

    public void setProgress(int progress) {
        int clamped = Math.max(LobbyMapResetRules.INACTIVE, Math.min(LobbyMapResetRules.COMPLETE, progress));
        if (clamped != this.progress) {
            this.progress = clamped;
            KEY.sync(scoreboard);
        }
    }

    /**
     * Optional on clients, like the anti-fast-round component: the server enforces everything on its own.
     * 与防速通组件一样对客户端可选：所有限制都由服务端独立执行。
     */
    @Override
    public boolean isRequiredOnClient() {
        return false;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(progress + 1);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.progress = Math.max(
                LobbyMapResetRules.INACTIVE,
                Math.min(LobbyMapResetRules.COMPLETE, buf.readVarInt() - 1)
        );
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }
}
