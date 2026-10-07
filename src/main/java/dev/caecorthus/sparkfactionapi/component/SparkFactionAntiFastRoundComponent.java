package dev.caecorthus.sparkfactionapi.component;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.impl.antifastround.AntiFastRoundRules;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * Server-wide anti-fast-round settings plus the live safe-time window. Only the settings are saved; the window is
 * runtime state shared with clients through a custom sync packet so they can predict the interaction lock. The window
 * end is a world time, which every dimension and the client report identically.
 * 全服的防速通局设置以及当前安全时间窗口。只持久化设置；窗口是运行时状态，通过自定义同步包发给客户端，
 * 让客户端可以预测交互锁。窗口结束点使用世界时间，各维度与客户端读到的值一致。
 */
public final class SparkFactionAntiFastRoundComponent implements AutoSyncedComponent {
    public static final ComponentKey<SparkFactionAntiFastRoundComponent> KEY = ComponentRegistry.getOrCreate(
            SparkFactionApiMod.id("anti_fast_round"),
            SparkFactionAntiFastRoundComponent.class
    );

    private static final String ENABLED_KEY = "Enabled";
    private static final String SECONDS_KEY = "Seconds";

    private final Scoreboard scoreboard;
    private boolean enabled;
    private int seconds = AntiFastRoundRules.DEFAULT_SECONDS;
    private long windowEnd = AntiFastRoundRules.NO_WINDOW;
    // Server-only bookkeeping for the delayed start notice; never synced or saved.
    // 仅服务端使用的延迟开局提示记录；不同步也不持久化。
    private long windowStart = AntiFastRoundRules.NO_WINDOW;
    private @Nullable RegistryKey<World> windowWorld;
    private boolean startNoticeSent;

    public SparkFactionAntiFastRoundComponent(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public SparkFactionAntiFastRoundComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this(scoreboard);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSeconds() {
        return seconds;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public @Nullable RegistryKey<World> getWindowWorld() {
        return windowWorld;
    }

    public boolean hasOpenWindow() {
        return windowEnd != AntiFastRoundRules.NO_WINDOW;
    }

    public boolean isStartNoticeSent() {
        return startNoticeSent;
    }

    public void markStartNoticeSent() {
        this.startNoticeSent = true;
    }

    public void setSettings(boolean enabled, int seconds) {
        this.enabled = enabled;
        this.seconds = AntiFastRoundRules.clampSeconds(seconds);
        sync();
    }

    public void openWindow(RegistryKey<World> world, long now, long windowEnd) {
        this.windowWorld = world;
        this.windowStart = now;
        this.windowEnd = windowEnd;
        this.startNoticeSent = false;
        sync();
    }

    /** Returns whether a window was open. / 返回此前是否存在窗口。 */
    public boolean closeWindow() {
        boolean wasOpen = hasOpenWindow();
        this.windowWorld = null;
        this.windowStart = AntiFastRoundRules.NO_WINDOW;
        this.windowEnd = AntiFastRoundRules.NO_WINDOW;
        this.startNoticeSent = false;
        if (wasOpen) {
            sync();
        }
        return wasOpen;
    }

    public void sync() {
        KEY.sync(scoreboard);
    }

    /**
     * Optional on clients: a client without this component ignores the sync instead of being disconnected by CCA;
     * the server still enforces the safe time on its own.
     * 客户端可选：缺少此组件的客户端会忽略同步而不是被 CCA 断开连接；服务端仍会独立执行安全时间。
     */
    @Override
    public boolean isRequiredOnClient() {
        return false;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(enabled);
        buf.writeVarInt(seconds);
        buf.writeLong(windowEnd);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.seconds = AntiFastRoundRules.clampSeconds(buf.readVarInt());
        this.windowEnd = buf.readLong();
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.enabled = tag.getBoolean(ENABLED_KEY);
        this.seconds = tag.contains(SECONDS_KEY, NbtElement.INT_TYPE)
                ? AntiFastRoundRules.clampSeconds(tag.getInt(SECONDS_KEY))
                : AntiFastRoundRules.DEFAULT_SECONDS;
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean(ENABLED_KEY, enabled);
        tag.putInt(SECONDS_KEY, seconds);
    }
}
