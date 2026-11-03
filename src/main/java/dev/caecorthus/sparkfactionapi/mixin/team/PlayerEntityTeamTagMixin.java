package dev.caecorthus.sparkfactionapi.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the Caecorthus team badge to player display names (chat, death messages, name tags, Wathe's crosshair name),
 * on both sides, in and out of rounds. Disguised crosshair names are matched up by the client's
 * {@code RoleNameRendererTeamTagMixin}.
 * 给玩家显示名（聊天、死亡消息、头顶名字、Wathe 准星下的名字）加 Caecorthus 团队标签，两端都生效，对局内外都加。
 * 准星名字被伪装替换时，由客户端的 {@code RoleNameRendererTeamTagMixin} 按显示的名字校正。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityTeamTagMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text sparkfactionapi$appendCaecorthusTeamTag(Text original) {
        return CaecorthusTeamTag.withTag(((PlayerEntity) (Object) this).getUuid(), original);
    }
}
