package dev.caecorthus.sparkfactionapi.mixin.text;

import dev.caecorthus.sparkfactionapi.impl.text.WatheTextRepairs;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes the role name on the round letter translate on each client (see
 * {@link WatheTextRepairs#translatableLetterRoleName}). Works on the finished lore at the tail of
 * {@code applyLetterLore}: Wathe builds the lines inside a lambda whose number differs between the two Wathe 1.5.6
 * builds, and other mods (SparkWitch's Dominator letter) also edit the finished lore.
 * 让开局信件上的身份名在每个客户端各自翻译（见 {@link WatheTextRepairs#translatableLetterRoleName}）。在
 * {@code applyLetterLore} 末尾改成品 lore：Wathe 在一个 lambda 里构造这些行，两个 Wathe 1.5.6 构建的 lambda 编号不同；
 * 其他模组（SparkWitch 的支配者信件）也是改成品 lore。
 */
@Mixin(GameFunctions.class)
public abstract class GameFunctionsLetterRoleNameMixin {
    @Inject(method = "applyLetterLore", at = @At("TAIL"))
    private static void sparkfactionapi$translatableLetterRoleName(
            ItemStack letter,
            ServerPlayerEntity player,
            Role role,
            String roleName,
            String factionName,
            int letterColor,
            int roleColor,
            CallbackInfo ci
    ) {
        LoreComponent lore = letter.get(DataComponentTypes.LORE);
        if (lore == null) {
            return;
        }
        List<Text> lines = WatheTextRepairs.translatableLetterRoleName(lore.lines(), roleName);
        if (lines != null) {
            letter.set(DataComponentTypes.LORE, new LoreComponent(lines));
        }
    }
}
