package dev.caecorthus.sparkfactionapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparkfactionapi.impl.replay.RoleChangeRecorder;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * Wathe's {@code addRole} overloads are plain map writes with no event; this Adapter records mid-round changes.
 * Both overloads are wrapped whole so the role is compared after other mods' HEAD cancels or rewrites
 * (e.g. SparkWitch Emma claims), and {@code readFromNbt} restores are excluded.
 * Wathe 的 {@code addRole} 两个重载只是写表、不发事件；本适配器记录对局内身份变化。整体包裹两个重载，
 * 使其他模组在 HEAD 取消或改写（如 SparkWitch Emma 唯一声明）后仍按实际结果比较，并排除 {@code readFromNbt} 恢复。
 */
@Mixin(value = GameWorldComponent.class, remap = false)
public abstract class GameWorldComponentReplayMixin {
    @Shadow
    @Final
    private World world;

    @WrapMethod(method = "readFromNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)V")
    private void sparkfactionapi$restoreRolesWithoutRecording(
            NbtCompound tag,
            RegistryWrapper.WrapperLookup lookup,
            Operation<Void> original
    ) {
        RoleChangeRecorder.beginRestore();
        try {
            original.call(tag, lookup);
        } finally {
            RoleChangeRecorder.endRestore();
        }
    }

    @WrapMethod(method = "addRole(Lnet/minecraft/entity/player/PlayerEntity;Ldev/doctor4t/wathe/api/Role;)V")
    private void sparkfactionapi$recordPlayerRoleChange(PlayerEntity player, Role role, Operation<Void> original) {
        UUID uuid = player == null ? null : player.getUuid();
        Role before = sparkfactionapi$roleOf(uuid);
        original.call(player, role);
        RoleChangeRecorder.afterRoleWrite(world, (GameWorldComponent) (Object) this, uuid, before);
    }

    @WrapMethod(method = "addRole(Ljava/util/UUID;Ldev/doctor4t/wathe/api/Role;)V")
    private void sparkfactionapi$recordUuidRoleChange(UUID player, Role role, Operation<Void> original) {
        Role before = sparkfactionapi$roleOf(player);
        original.call(player, role);
        RoleChangeRecorder.afterRoleWrite(world, (GameWorldComponent) (Object) this, player, before);
    }

    @Unique
    private Role sparkfactionapi$roleOf(UUID player) {
        return player == null ? null : ((GameWorldComponent) (Object) this).getRole(player);
    }
}
