package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets Wathe's letter fallback see custom faction suffixes.
 * 让 wathe 信件 fallback 能识别自定义阵营后缀。
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
        applyLetterLore(letter, player, role, roleName, letterFactionName(role, factionName), letterColor, roleColor);
    }

    private static String letterFactionName(Role role, String fallback) {
        Identifier factionId = SparkFactionApi.resolveBaseFaction(role);
        FactionDefinition definition = SparkFactionApi.getFaction(factionId).orElse(null);
        if (definition == null || factionId.getNamespace().equals("wathe")) {
            return fallback;
        }
        return factionId.getNamespace() + "." + factionId.getPath().replace('/', '.');
    }
}
