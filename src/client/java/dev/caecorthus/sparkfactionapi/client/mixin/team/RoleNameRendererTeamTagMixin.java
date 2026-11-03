package dev.caecorthus.sparkfactionapi.client.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.caecorthus.sparkfactionapi.client.team.ShownPlayerNames;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the badge on Wathe's crosshair name match the name actually drawn. Disguise mixins (NoellesRoles and
 * SparkStrength Morphling, Coroner, Myriad scramble) wrap the {@code getDisplayName} call and may return another
 * player's name as plain text; reading the field after all of them lets the badge follow whoever is imitated.
 * 让 Wathe 准星名字上的团队标签和最终画出来的名字一致。伪装类 mixin（NoellesRoles 与 SparkStrength 的变形者、验尸官、
 * 千面混淆）包装了 {@code getDisplayName} 调用，可能返回别人名字的纯文本；在它们之后读字段时校正，标签就跟着被模仿的人走。
 */
@Mixin(RoleNameRenderer.class)
public abstract class RoleNameRendererTeamTagMixin {
    @ModifyExpressionValue(
            method = "renderHud",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/client/gui/RoleNameRenderer;nametag:Lnet/minecraft/text/Text;",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private static Text sparkfactionapi$matchTeamTagToShownName(Text shown) {
        return CaecorthusTeamTag.withTagForShownName(shown, ShownPlayerNames::uuidOf);
    }
}
