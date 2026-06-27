package dev.caecorthus.sparkfactionapi.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        ServerPlayerEntity moneyRecipient = killer;
        if (moneyRecipient == null) {
            PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(victim);
            MinecraftServer server = victim.getServer();
            if (poisonComponent.poisoner != null && server != null) {
                moneyRecipient = server.getPlayerManager().getPlayer(poisonComponent.poisoner);
            }
        }
        if (moneyRecipient == null) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(moneyRecipient.getWorld());
        if (FactionCapabilityBridge.receivesCustomKillReward(moneyRecipient, gameComponent)) {
            PlayerShopComponent.KEY.get(moneyRecipient).addToBalance(GameConstants.MONEY_PER_KILL);
        }
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
