package dev.caecorthus.sparkfactionapi.mixin.record;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparkfactionapi.impl.record.ConsumeRecorder;
import dev.caecorthus.sparkfactionapi.impl.record.PlayerScopeStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Funnel for finished eat/drink uses: vanilla {@code LivingEntity#consumeItem} calls {@code ItemStack#finishUsing},
 * which dispatches to the item's own {@code finishUsing} override. Wrapping the whole method reads the item before an
 * override can empty the stack and records only after it returned having used or replaced the stack. The result is
 * passed through unchanged.
 * 吃/喝完成的汇聚点：原版 {@code LivingEntity#consumeItem} 调用 {@code ItemStack#finishUsing}，再分派到物品自己的
 * {@code finishUsing} 重写。整体包裹该方法，以便在重写清空物品堆之前读取物品，并仅在其返回且物品堆被消耗或替换后记录。
 * 返回值原样传递。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackConsumeMixin {
    @WrapMethod(method = "finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;")
    private ItemStack sparkfactionapi$recordConsume(World world, LivingEntity user, Operation<ItemStack> original) {
        ItemStack self = (ItemStack) (Object) this;
        PlayerScopeStack.Frame<ConsumeRecorder.Use> frame = ConsumeRecorder.beginUse(self, world, user);
        ItemStack result = null;
        try {
            result = original.call(world, user);
            return result;
        } finally {
            // result stays null when finishUsing threw. 若 finishUsing 抛出异常则 result 仍为 null。
            ConsumeRecorder.endUse(frame, user, self, result);
        }
    }
}
