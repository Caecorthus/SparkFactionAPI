package dev.caecorthus.sparkfactionapi.client.replay;

import dev.caecorthus.sparkfactionapi.command.replay.ReplayCommand;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshotPayload;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.client.gui.screen.MapVotingScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Client glue for the replay screen: snapshot receiver, open-replay keybind, and the button on Wathe's
 * map-voting screen. Every request path runs {@code /replay}; only the server's snapshot opens the screen.
 * 回放界面的客户端接线：快照接收、打开回放按键、Wathe 地图投票界面上的按钮。所有请求都走 {@code /replay}，
 * 只有服务端下发的快照才会打开界面。
 */
public final class ReplayClient {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 8;
    private static final int BUTTON_MIN_WIDTH = 60;
    private static final int BUTTON_MAX_WIDTH = 120;
    // Key auto-repeat and double clicks would otherwise spam /replay (and its "no replay" error).
    // 防止按键连发或连点刷屏 /replay（以及"暂无回放"报错）。
    private static final int REQUEST_COOLDOWN_TICKS = 10;

    private static KeyBinding openReplayKey;
    private static int requestCooldown;

    // ReplayScreen does not expose its parent, so remember what we handed the last one we opened. Every use
    // checks screen identity, so a stale entry is harmless; it is kept (not cleared) so a replay that opened
    // its own child screen and came back is still recognised.
    // ReplayScreen 不暴露 parent，这里记住最近一次打开时传入的值。每次使用都会比对界面身份，旧记录无害；
    // 不主动清除，以便回放界面打开子界面再返回后仍能被识别。
    private static @Nullable OpenedReplay opened;
    // The replay screen removed since the last tick start. Screen#removed and the next screen's init run
    // synchronously inside one setScreen call, so AFTER_INIT can tell which screen replaced it.
    // 自上次 tick 开始以来被移除的回放界面。removed 与下一个界面的 init 在同一次 setScreen 内同步发生，
    // 因此 AFTER_INIT 能判断是谁替换了它。
    private static @Nullable ReplayScreen removedReplay;

    private ReplayClient() {
    }

    public static void register() {
        // Fabric already runs play payload handlers on the render thread (ClientPlayNetworkAddon wraps them in client.execute).
        // Fabric 已在渲染线程上执行 play 数据包处理器（ClientPlayNetworkAddon 用 client.execute 包装），无需再切线程。
        ClientPlayNetworking.registerGlobalReceiver(ReplaySnapshotPayload.ID,
                (payload, context) -> onSnapshot(context.client(), payload.snapshot()));

        openReplayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sparkfactionapi.open_replay",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                "key.categories.sparkfactionapi"
        ));

        ClientTickEvents.START_CLIENT_TICK.register(ReplayClient::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(ReplayClient::onEndTick);
        ScreenEvents.AFTER_INIT.register(ReplayClient::afterScreenInit);
    }

    private static void onStartTick(MinecraftClient client) {
        // A removal not followed by an init (setScreen(null)) must not leak into a later screen swap.
        // 未紧跟 init 的移除（setScreen(null)）不能影响之后的界面切换。
        removedReplay = null;
    }

    private static void onEndTick(MinecraftClient client) {
        if (requestCooldown > 0) {
            requestCooldown--;
        }
        // Drain every press, but send at most one request per tick. Keybinds only fire with no screen open.
        // 消耗全部按下次数，但每 tick 至多发送一次请求。按键只在没有打开界面时触发。
        boolean pressed = false;
        while (openReplayKey.wasPressed()) {
            pressed = true;
        }
        if (pressed) {
            requestReplay(client);
        }
    }

    private static void requestReplay(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.getNetworkHandler() == null || requestCooldown > 0) {
            return;
        }
        requestCooldown = REQUEST_COOLDOWN_TICKS;
        player.networkHandler.sendChatCommand(ReplayCommand.LITERAL);
    }

    private static void onSnapshot(MinecraftClient client, ReplaySnapshot snapshot) {
        boolean inWorld = client.player != null && client.world != null && client.getNetworkHandler() != null;
        MapVotingComponent voting = inWorld ? voting(client) : null;
        boolean votingActive = voting != null && voting.isVotingActive();
        boolean roulette = votingActive && voting.isRoulettePhase();
        Screen current = client.currentScreen;

        OpenPolicy.ParentChoice choice = OpenPolicy.choose(inWorld, roulette, classify(current));
        if (choice == OpenPolicy.ParentChoice.IGNORE) {
            return;
        }
        Screen parent = switch (choice) {
            case CURRENT_SCREEN -> current;
            case PREVIOUS_REPLAY_PARENT -> opened != null && opened.screen() == current ? opened.parent() : null;
            case NO_PARENT, IGNORE -> null;
        };
        if (!OpenPolicy.parentStillValid(classify(parent), votingActive)) {
            parent = null;
        }
        open(client, snapshot, parent);
    }

    private static void open(MinecraftClient client, ReplaySnapshot snapshot, @Nullable Screen parent) {
        ReplayScreen screen = new ReplayScreen(snapshot, parent);
        client.setScreen(screen);
        opened = new OpenedReplay(screen, snapshot, parent);
    }

    private static void afterScreenInit(MinecraftClient client, Screen screen, int width, int height) {
        ReplayScreen removed = removedReplay;
        removedReplay = null;

        if (screen instanceof ReplayScreen replay) {
            // Fabric recreates per-screen events on every init/resize, so re-register each time.
            // Fabric 每次 init/resize 都会重建单界面事件，因此每次都重新注册。
            ScreenEvents.remove(replay).register(s -> removedReplay = replay);
            return;
        }
        if (screen instanceof MapVotingScreen votingScreen) {
            addVotingButton(client, votingScreen, width, height);
            restoreReplayIfDisplaced(client, votingScreen, removed);
        }
    }

    /**
     * Wathe auto-opens its voting screen once after the end-of-round animation and replaces whatever is open,
     * which would swallow a replay opened from the round-end chat button. Put the replay back on top, with the
     * voting screen as its parent. Not during the roulette: Wathe re-forces the voting screen every tick then.
     * Wathe 在结算动画后会自动打开一次投票界面并替换当前界面，会吞掉通过结算聊天按钮打开的回放。
     * 这里把回放放回最上层，并以投票界面作为 parent。轮盘阶段不处理：那时 Wathe 每 tick 都会强制打开投票界面。
     */
    private static void restoreReplayIfDisplaced(MinecraftClient client, MapVotingScreen votingScreen,
                                                 @Nullable ReplayScreen removed) {
        OpenedReplay previous = opened;
        boolean removedOurs = previous != null && removed != null && previous.screen() == removed;
        boolean backToParent = previous != null && previous.parent() == votingScreen;
        MapVotingComponent voting = voting(client);
        boolean votingActive = voting != null && voting.isVotingActive();
        boolean roulette = votingActive && voting.isRoulettePhase();
        if (!OpenPolicy.shouldRestoreReplay(removedOurs, backToParent, votingActive, roulette)) {
            return;
        }
        ReplaySnapshot snapshot = previous.snapshot();
        // Defer: we are still inside Wathe's setScreen call. send() always queues, unlike execute() on this thread.
        // 延后执行：此时仍在 Wathe 的 setScreen 调用内部。与本线程上的 execute() 不同，send() 总是排队。
        client.send(() -> {
            if (client.currentScreen == votingScreen) {
                open(client, snapshot, votingScreen);
            }
        });
    }

    private static void addVotingButton(MinecraftClient client, MapVotingScreen screen, int width, int height) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        // Wathe's re-init clears children today; stay idempotent in case that changes.
        // 目前 Wathe 重新 init 会清空子元素；这里保持幂等以防将来变化。
        buttons.removeIf(button -> button instanceof OpenReplayButton);

        Text label = Text.translatable("gui.sparkfactionapi.replay.open");
        int buttonWidth = MathHelper.clamp(client.textRenderer.getWidth(label) + 20, BUTTON_MIN_WIDTH, BUTTON_MAX_WIDTH);
        // Bottom-right, below Wathe's lower brass bars (height-43..height-39) and right of its centered page
        // indicator; ticket cards stop above height-45 even with the slide-in overshoot.
        // 右下角：位于 Wathe 底部黄铜装饰线（height-43..height-39）下方、居中页码右侧；
        // 车票卡片即使在滑入回弹时也不会低于 height-45 一带。
        OpenReplayButton button = new OpenReplayButton(
                width - buttonWidth - BUTTON_MARGIN,
                height - BUTTON_HEIGHT - BUTTON_MARGIN,
                buttonWidth,
                label,
                pressed -> requestReplay(client)
        );
        button.visible = votingButtonVisible(client);
        buttons.add(button);
        // MapVotingScreen#render draws nothing once voting ends, and the roulette would immediately replace a
        // replay, so the button only shows (and accepts clicks) while voting is open.
        // 投票结束后 MapVotingScreen#render 不再绘制内容，轮盘阶段又会立即替换回放界面，
        // 因此按钮只在投票进行中显示（并响应点击）。
        ScreenEvents.afterTick(screen).register(s -> button.visible = votingButtonVisible(client));
    }

    private static boolean votingButtonVisible(MinecraftClient client) {
        MapVotingComponent voting = voting(client);
        return voting != null && voting.isVotingActive() && !voting.isRoulettePhase();
    }

    private static @Nullable MapVotingComponent voting(MinecraftClient client) {
        return client.world == null ? null : MapVotingComponent.KEY.getNullable(client.world.getScoreboard());
    }

    private static OpenPolicy.ScreenKind classify(@Nullable Screen screen) {
        if (screen == null) {
            return OpenPolicy.ScreenKind.NONE;
        }
        if (screen instanceof DownloadingTerrainScreen) {
            return OpenPolicy.ScreenKind.LOADING;
        }
        if (screen instanceof MapVotingScreen) {
            return OpenPolicy.ScreenKind.MAP_VOTING;
        }
        if (screen instanceof ChatScreen) {
            return OpenPolicy.ScreenKind.CHAT;
        }
        if (screen instanceof ReplayScreen) {
            return OpenPolicy.ScreenKind.REPLAY;
        }
        return OpenPolicy.ScreenKind.OTHER;
    }

    private record OpenedReplay(ReplayScreen screen, ReplaySnapshot snapshot, @Nullable Screen parent) {
    }

    /** Marker subclass so re-init can find and drop our own button. 标记子类，便于重新 init 时找到并移除自己的按钮。 */
    private static final class OpenReplayButton extends ButtonWidget {
        private OpenReplayButton(int x, int y, int width, Text message, PressAction onPress) {
            super(x, y, width, BUTTON_HEIGHT, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        }
    }

    /**
     * Pure open/parent decisions, kept free of Minecraft types so unit tests can load them without a client.
     * 纯打开/parent 决策，不依赖 Minecraft 类型，单元测试无需客户端即可加载。
     */
    static final class OpenPolicy {
        enum ScreenKind { NONE, LOADING, MAP_VOTING, CHAT, REPLAY, OTHER }

        enum ParentChoice { IGNORE, CURRENT_SCREEN, NO_PARENT, PREVIOUS_REPLAY_PARENT }

        private OpenPolicy() {
        }

        /**
         * Where a freshly received snapshot opens. Voting screen: return to voting. Chat (the round-end chat
         * button): return to the game, not the chat box. Replay: replace it but keep its parent.
         * 新快照的打开方式。投票界面：关闭后回到投票。聊天栏（结算聊天按钮）：关闭后回到游戏而非聊天栏。
         * 回放界面：替换它但保留原 parent。
         */
        static ParentChoice choose(boolean inWorld, boolean roulettePhase, ScreenKind current) {
            if (!inWorld || current == ScreenKind.LOADING) {
                return ParentChoice.IGNORE;
            }
            // Wathe forces its voting screen back every tick during the roulette; a replay would vanish at once.
            // 轮盘阶段 Wathe 每 tick 都会强制切回投票界面，回放会立刻消失。
            if (roulettePhase) {
                return ParentChoice.IGNORE;
            }
            return switch (current) {
                case MAP_VOTING, OTHER -> ParentChoice.CURRENT_SCREEN;
                case NONE, CHAT -> ParentChoice.NO_PARENT;
                case REPLAY -> ParentChoice.PREVIOUS_REPLAY_PARENT;
                case LOADING -> ParentChoice.IGNORE;
            };
        }

        /**
         * A voting screen is a dead end once voting is over (Wathe closes it on the next tick), so drop it as parent.
         * 投票结束后投票界面已无意义（Wathe 会在下一 tick 关闭它），因此不再作为 parent。
         */
        static boolean parentStillValid(ScreenKind parentKind, boolean votingActive) {
            return parentKind != ScreenKind.MAP_VOTING || votingActive;
        }

        static boolean shouldRestoreReplay(boolean removedOurReplay, boolean returningToItsParent,
                                           boolean votingActive, boolean roulettePhase) {
            return removedOurReplay && !returningToItsParent && votingActive && !roulettePhase;
        }
    }
}
