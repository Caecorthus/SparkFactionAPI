package dev.caecorthus.sparkfactionapi.client;

import dev.caecorthus.sparkfactionapi.api.FactionInstinctPolicy;
import dev.caecorthus.sparkfactionapi.impl.FactionCapabilityBridge;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public final class SparkFactionApiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Bridge through Wathe's event so other add-ons can still win by priority.
        // 通过 wathe 事件桥接，保留下游模组按优先级覆盖的空间。
        GetInstinctHighlight.EVENT.register(target -> {
            PlayerEntity viewer = MinecraftClient.getInstance().player;
            if (viewer == null) {
                return null;
            }

            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            return FactionCapabilityBridge.instinctPolicyResult(viewer, target, gameComponent)
                    .map(SparkFactionApiClient::toWatheHighlight)
                    .orElseGet(() -> fallbackInstinctHighlight(viewer, target, gameComponent));
        });

        // Cohort display is visual only; it must not put custom factions into native killer teams.
        // 同伙显示只影响视觉提示，不能把自定义阵营塞进原生杀手队伍。
        ShouldShowCohort.EVENT.register((viewer, target) -> {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            if (FactionCapabilityBridge.sharesCohort(viewer, target, gameComponent)) {
                return ShouldShowCohort.CohortResult.show();
            }
            return null;
        });
    }

    private static GetInstinctHighlight.HighlightResult toWatheHighlight(
            FactionInstinctPolicy.InstinctResult result
    ) {
        if (result.skip()) {
            return GetInstinctHighlight.HighlightResult.skip();
        }
        if (result.requiresKeybind()) {
            return GetInstinctHighlight.HighlightResult.withKeybind(result.color(), result.priority());
        }
        return GetInstinctHighlight.HighlightResult.always(result.color(), result.priority());
    }

    private static GetInstinctHighlight.HighlightResult fallbackInstinctHighlight(
            PlayerEntity viewer,
            net.minecraft.entity.Entity target,
            GameWorldComponent gameComponent
    ) {
        if (!(target instanceof PlayerEntity targetPlayer)) {
            return null;
        }
        if (!FactionCapabilityBridge.hasCustomEffectiveFaction(viewer, gameComponent)) {
            return null;
        }
        if (!FactionCapabilityBridge.canUseInstinct(viewer, gameComponent)) {
            return null;
        }
        if (GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
            return GetInstinctHighlight.HighlightResult.skip();
        }
        int color = FactionCapabilityBridge.instinctDisplayColor(targetPlayer, gameComponent);
        if (color == -1) {
            return null;
        }
        return GetInstinctHighlight.HighlightResult.withKeybind(color);
    }
}
