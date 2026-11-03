package dev.caecorthus.sparkfactionapi.client.mapreset;

import dev.caecorthus.sparkfactionapi.component.SparkFactionMapResetComponent;
import dev.caecorthus.sparkfactionapi.impl.mapreset.LobbyMapResetRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Lobby HUD line for the gradual map reset: the percentage while it runs, then "complete" for as long as the server
 * holds 100. Drawn for every player in the lobby, never during a round.
 * 渐进式地图重置的大厅 HUD 行：进行中显示百分比，服务端保持 100 期间显示"完成"。大厅内所有玩家可见，对局中不绘制。
 */
public final class MapResetHud {
    /** Wathe's lobby gold, so the line reads as part of its lobby HUD. / Wathe 大厅金色，使这一行与其大厅 HUD 一致。 */
    private static final int PROGRESS_COLOR = 0xFFC5A244;
    private static final int COMPLETE_COLOR = 0xFF55FF55;
    private static final int BAR_TRACK_COLOR = 0x80000000;
    /**
     * Wathe's lobby HUD is translated to y=6; its lowest line is the voting keybind hint at +28 (9 px font), ending at
     * y=43. The idle lobby lines end higher (+20 → y=35). 48 leaves a 5 px gap in both phases.
     * Wathe 大厅 HUD 平移到 y=6；最低的一行是投票按键提示（+28，字高 9 像素），止于 y=43。
     * 普通大厅信息止于更高处（+20 → y=35）。取 48 可在两种阶段下都留出 5 像素间距。
     */
    private static final int TEXT_Y = 48;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_GAP = 2;

    // Render-thread cache so a steady percentage does not build a new Text every frame.
    // 渲染线程缓存：百分比不变时不必每帧新建 Text。
    private static int cachedProgress = LobbyMapResetRules.INACTIVE;
    private static Text cachedText;

    private MapResetHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(MapResetHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        // Fabric draws this after the Tab list, unlike Wathe's lobby lines, so step aside while it is open.
        // Fabric 在 Tab 列表之后绘制此回调（Wathe 大厅文字在其下方），因此列表打开时让开。
        if (player == null || client.world == null || client.options.hudHidden
                || client.options.playerListKey.isPressed()) {
            return;
        }
        // getNullable: the component is optional on clients, so a mismatched server may not sync it.
        // getNullable：该组件对客户端可选，版本不一致的服务端可能不会同步它。
        SparkFactionMapResetComponent reset = SparkFactionMapResetComponent.KEY.getNullable(client.world.getScoreboard());
        if (reset == null) {
            return;
        }
        int progress = reset.getProgress();
        boolean complete = progress == LobbyMapResetRules.COMPLETE;
        if (!complete && !LobbyMapResetRules.isResetting(progress)) {
            return;
        }
        if (GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
            return;
        }

        Text text = textFor(progress, complete);
        int color = complete ? COMPLETE_COLOR : PROGRESS_COLOR;
        TextRenderer renderer = client.textRenderer;
        int centerX = context.getScaledWindowWidth() / 2;
        context.drawTextWithShadow(renderer, text, centerX - renderer.getWidth(text) / 2, TEXT_Y, color);

        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = TEXT_Y + renderer.fontHeight + BAR_GAP;
        context.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, BAR_TRACK_COLOR);
        int filled = BAR_WIDTH * progress / LobbyMapResetRules.COMPLETE;
        if (filled > 0) {
            context.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, color);
        }
    }

    private static Text textFor(int progress, boolean complete) {
        if (cachedText == null || cachedProgress != progress) {
            cachedText = complete
                    ? Text.translatableWithFallback("hud.sparkfactionapi.map_reset.complete", "Map reset complete")
                    : Text.translatableWithFallback("hud.sparkfactionapi.map_reset.progress", "Resetting map %s%%", progress);
            cachedProgress = progress;
        }
        return cachedText;
    }
}
