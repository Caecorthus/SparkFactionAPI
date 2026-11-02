package dev.caecorthus.sparkfactionapi.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import dev.doctor4t.wathe.game.GameFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps name badges off the room letter. Wathe only strips one trailing Ratatouille glyph, which fails once the
 * Caecorthus badge sits after it.
 * 信件上的名字不带图标。Wathe 只去掉结尾的一个 Ratatouille 图标，Caecorthus 标签排在它后面时就去不掉了。
 */
@Mixin(GameFunctions.class)
public abstract class GameFunctionsLetterNameMixin {
    @ModifyReturnValue(method = "getPlayerDisplayName", at = @At("RETURN"))
    private static String sparkfactionapi$stripNameBadges(String original) {
        return CaecorthusTeamTag.stripGlyphs(original);
    }
}
