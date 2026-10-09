package dev.caecorthus.sparkfactionapi.mixin.record;

import dev.caecorthus.sparkfactionapi.impl.record.DeathRecordEnricher;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adds the kill-start fields to the data of the same {@code death} event, right before {@code recordDeath} hands it
 * to {@code addEvent} (which copies it into the match record). No extra event is written.
 * 在 {@code recordDeath} 把数据交给 {@code addEvent}（会复制进对局记录）之前，把击杀开始时的字段加到同一个
 * {@code death} 事件的数据中。不另写事件。
 */
@Mixin(value = GameRecordManager.class, remap = false)
public abstract class GameRecordManagerDeathMixin {
    @ModifyArg(
            method = "recordDeath(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/record/GameRecordManager;addEvent(Lnet/minecraft/server/world/ServerWorld;Ljava/lang/String;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/nbt/NbtCompound;)V"
            ),
            index = 4
    )
    private static NbtCompound sparkfactionapi$addKillStartFields(
            ServerWorld world,
            String type,
            ServerPlayerEntity killer,
            ServerPlayerEntity victim,
            NbtCompound data
    ) {
        // recordDeath passes (world, "death", killer as actor, victim as target, data).
        // recordDeath 传入 (world, "death", 凶手作为 actor, 受害者作为 target, data)。
        return DeathRecordEnricher.enrich(victim, killer, data);
    }
}
