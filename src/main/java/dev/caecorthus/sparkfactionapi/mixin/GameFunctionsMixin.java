package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.impl.economy.FactionKillRewardAdapter;
import dev.caecorthus.sparkfactionapi.impl.text.FactionLetterTextRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapts Wathe game-function hooks to custom faction text and economy Modules.
 * 将 Wathe 游戏函数钩子适配到自定义阵营文本与经济模块。
 */
@Mixin(GameFunctions.class)
public abstract class GameFunctionsMixin {
    @Shadow
    private static void applyLetterLore(
            ItemStack letter,
            ServerPlayerEntity player,
            Role role,
            String roleName,
            String factionName,
            int letterColor,
            int roleColor
    ) {
        throw new AssertionError();
    }

    @Redirect(
            method = "giveLettersToPlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;applyLetterLore(Lnet/minecraft/item/ItemStack;Lnet/minecraft/server/network/ServerPlayerEntity;Ldev/doctor4t/wathe/api/Role;Ljava/lang/String;Ljava/lang/String;II)V"
            )
    )
    private static void sparkfactionapi$applyCustomFactionLetterLore(
            ItemStack letter,
            ServerPlayerEntity player,
            Role role,
            String roleName,
            String factionName,
            int letterColor,
            int roleColor
    ) {
        applyLetterLore(
                letter,
                player,
                role,
                roleName,
                FactionLetterTextRules.letterFactionName(role, factionName),
                letterColor,
                roleColor
        );
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;reset()V"
            )
    )
    private static void sparkfactionapi$applyCustomKillReward(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force,
            CallbackInfo ci
    ) {
        FactionKillRewardAdapter.applyCustomKillReward(victim, killer);
    }
}
