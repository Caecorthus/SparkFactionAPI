package dev.caecorthus.sparkfactionapi.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the Caecorthus team badge to player display names (chat, death messages, name tags, Wathe's crosshair name),
 * on both sides, outside running rounds.
 * 给玩家显示名（聊天、死亡消息、头顶名字、Wathe 准星下的名字）加 Caecorthus 团队标签，两端都生效，一局进行中不加。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityTeamTagMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text sparkfactionapi$appendCaecorthusTeamTag(Text original) {
        return CaecorthusTeamTag.withTagOutsideRound((PlayerEntity) (Object) this, original);
    }
}
