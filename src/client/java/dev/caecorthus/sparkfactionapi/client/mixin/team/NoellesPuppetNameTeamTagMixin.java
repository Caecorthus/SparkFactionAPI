package dev.caecorthus.sparkfactionapi.client.mixin.team;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparkfactionapi.impl.team.CaecorthusTeamTag;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Gives a Puppeteer's puppet the team badge of the dead player it looks like. NoellesRoles draws the puppet's name
 * itself from the player list as plain text, so it would otherwise lack the badge that player had alive. Only builds
 * of NoellesRoles with the Puppeteer have this class; elsewhere the mixin does nothing.
 * 让傀儡师的傀儡带上它所扮演死者的团队标签。NoellesRoles 自己从玩家列表取纯文本名字画傀儡名牌，否则会缺少那位玩家生前的标签。
 * 只有带傀儡师的 NoellesRoles 版本有这个类，其他版本里本 mixin 不生效。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.client.puppeteer.PuppeteerClient", remap = false)
public abstract class NoellesPuppetNameTeamTagMixin {
    @ModifyReturnValue(method = "getName", at = @At("RETURN"), require = 0, remap = false)
    private static Text sparkfactionapi$appendCaecorthusTeamTag(Text original, UUID uuid) {
        return CaecorthusTeamTag.withTag(uuid, original);
    }
}
