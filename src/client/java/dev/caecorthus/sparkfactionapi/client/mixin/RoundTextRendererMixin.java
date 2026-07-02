package dev.caecorthus.sparkfactionapi.client.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.caecorthus.sparkfactionapi.impl.FactionRoundEndTextRules;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Shows custom faction wins with faction text instead of the first winning role's text.
 * 自定义阵营胜利时显示阵营公告，而不是第一个胜者角色的公告。
 */
@Mixin(RoundTextRenderer.class)
public abstract class RoundTextRendererMixin {
    @Redirect(
            method = "renderHud",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/client/gui/RoleAnnouncementTexts$RoleAnnouncementText;winText:Lnet/minecraft/text/Text;"
            )
    )
    private static Text sparkfactionapi$customFactionWinTitle(
            RoleAnnouncementTexts.RoleAnnouncementText roleText,
            TextRenderer renderer,
            ClientPlayerEntity player,
            DrawContext context
    ) {
        return customWinningFaction(player)
                .map(FactionRoundEndTextRules::winTitle)
                .map(Text.class::cast)
                .orElse(roleText.winText);
    }

    @Redirect(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Identifier;getPath()Ljava/lang/String;"
            )
    )
    private static String sparkfactionapi$customFactionWinPhrasePath(
            Identifier roleId,
            TextRenderer renderer,
            ClientPlayerEntity player,
            DrawContext context
    ) {
        return customWinningFaction(player)
                .map(FactionDefinition::id)
                .map(FactionRoundEndTextRules::winPhrasePath)
                .orElseGet(roleId::getPath);
    }

    private static Optional<FactionDefinition> customWinningFaction(ClientPlayerEntity player) {
        if (player == null) {
            return Optional.empty();
        }
        SparkFactionRoundEndComponent roundEnd = SparkFactionRoundEndComponent.KEY.get(player.getScoreboard());
        Identifier winningFaction = roundEnd.getWinningFaction();
        if (winningFaction == null || !roundEnd.hasCustomWin()) {
            return Optional.empty();
        }
        return SparkFactionApi.getFaction(winningFaction);
    }
}
