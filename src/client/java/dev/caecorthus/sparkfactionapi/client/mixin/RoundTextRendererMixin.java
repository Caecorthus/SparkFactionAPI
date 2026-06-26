package dev.caecorthus.sparkfactionapi.client.mixin;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays custom faction win text after Wathe draws the normal round-end HUD.
 * 在 wathe 原生结算 HUD 绘制后覆盖自定义阵营胜利文案，避免扩展 WinStatus。
 */
@Mixin(RoundTextRenderer.class)
public abstract class RoundTextRendererMixin {
    @Shadow
    private static int endTime;

    @Inject(method = "renderHud", at = @At("RETURN"))
    private static void sparkfactionapi$renderCustomFactionWin(
            TextRenderer renderer,
            ClientPlayerEntity player,
            DrawContext context,
            CallbackInfo ci
    ) {
        if (player == null || endTime <= 0 || endTime >= 120) {
            return;
        }
        if (GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
            return;
        }

        SparkFactionRoundEndComponent roundEnd = SparkFactionRoundEndComponent.KEY.get(player.getScoreboard());
        Identifier factionId = roundEnd.getWinningFaction();
        if (factionId == null) {
            return;
        }

        FactionDefinition definition = SparkFactionApi.getFaction(factionId).orElse(null);
        int color = definition == null ? 0xFFFFFF : definition.color();
        String keySuffix = translationSuffix(factionId);
        Text winTitle = Text.translatable("announcement.win." + keySuffix);
        Text winSubtitle = Text.translatable("game.win." + keySuffix);

        context.getMatrices().push();
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2.0F,
                context.getScaledWindowHeight() / 2.0F - 40.0F,
                0.0F
        );

        context.getMatrices().push();
        context.getMatrices().scale(2.6F, 2.6F, 1.0F);
        context.drawTextWithShadow(renderer, winTitle, -renderer.getWidth(winTitle) / 2, -12, color);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().scale(1.2F, 1.2F, 1.0F);
        context.drawTextWithShadow(renderer, winSubtitle, -renderer.getWidth(winSubtitle) / 2, 14, 0xFFFFFF);
        context.getMatrices().pop();

        context.getMatrices().pop();
    }

    private static String translationSuffix(Identifier id) {
        return id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
