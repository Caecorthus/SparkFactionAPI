package dev.caecorthus.sparkfactionapi.component;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.caecorthus.sparkfactionapi.impl.thirdfaction.ThirdFactionDrawSettings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

/**
 * Server-wide third-faction draw settings ({@link ThirdFactionDrawSettings}), saved with the scoreboard. Server-only:
 * it is not an {@code AutoSyncedComponent}, so nothing reaches clients. Written by
 * {@code /wathe:gameSettings set roleDividend third_faction} and through {@code SparkFactionApi} (SparkWitch's
 * minimum-players command); read once per round by the draw.
 * 全服第三方阵营抽取设置（{@link ThirdFactionDrawSettings}），随计分板保存。仅服务端：不是 {@code AutoSyncedComponent}，
 * 不会同步到客户端。由 {@code /wathe:gameSettings set roleDividend third_faction} 及 {@code SparkFactionApi}
 * （SparkWitch 的最低人数命令）写入；每局抽取时读取一次。
 */
public final class SparkFactionThirdFactionComponent implements Component {
    public static final ComponentKey<SparkFactionThirdFactionComponent> KEY = ComponentRegistry.getOrCreate(
            SparkFactionApiMod.id("third_faction_draw"),
            SparkFactionThirdFactionComponent.class
    );

    private volatile ThirdFactionDrawSettings settings = ThirdFactionDrawSettings.DEFAULTS;

    public SparkFactionThirdFactionComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
    }

    public static SparkFactionThirdFactionComponent of(MinecraftServer server) {
        return KEY.get(server.getScoreboard());
    }

    public ThirdFactionDrawSettings settings() {
        return settings;
    }

    /** @throws IllegalArgumentException dividend below 3 除数小于 3 */
    public void setDividend(int dividend) {
        this.settings = settings.withDividend(dividend);
    }

    /** @throws IllegalArgumentException players below 0 人数小于 0 */
    public void setMinPlayers(int minPlayers) {
        this.settings = settings.withMinPlayers(minPlayers);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.settings = ThirdFactionDrawSettings.fromNbt(tag);
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        settings.writeNbt(tag);
    }
}
