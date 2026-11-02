package dev.caecorthus.sparkfactionapi.client.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the Caecorthus team badge to Tab list names. The list shows real accounts, so it stays on during rounds.
 * 给 Tab 列表里的名字加 Caecorthus 团队标签。列表本来就是真实账号，一局进行中也显示。
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudTeamTagMixin {
    @ModifyReturnValue(method = "applyGameModeFormatting", at = @At("RETURN"))
    private Text sparkfactionapi$appendCaecorthusTeamTag(Text original, PlayerListEntry entry, MutableText name) {
        return CaecorthusTeamTag.withTag(entry.getProfile().getId(), original);
    }
}
