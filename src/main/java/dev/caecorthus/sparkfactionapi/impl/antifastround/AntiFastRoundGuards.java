package dev.caecorthus.sparkfactionapi.impl.antifastround;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

/**
 * Safe-time interaction lock, copied from SparkWitch's Control Expert stun guards and SparkStrength's Taotie daze
 * guards. Server-authoritative; it also runs on the client, where FAIL means no packet is sent. The listeners sit in a
 * dedicated phase ordered before {@link Event#DEFAULT_PHASE}, so they decide before every default-phase listener,
 * including add-on callbacks that kill or apply knockback inside the callback. Other mods' early lock phases are
 * unordered relative to this one, which is harmless because each only returns FAIL or PASS. Unrestricted players
 * always get PASS, so the relative order of all other listeners is unchanged. Bare-hand block use (doors, buttons)
 * and bare-hand non-player entity use (seats) stay allowed; every entity attack is denied, which is the "no knockback"
 * guarantee.
 * 安全时间交互锁，照搬 SparkWitch 控场专家眩晕与 SparkStrength 饕餮眩晕的写法。服务端权威；客户端同样运行，FAIL 时
 * 不会发送数据包。监听器位于排在 {@link Event#DEFAULT_PHASE} 之前的专用阶段，先于所有默认阶段监听器做出决定，包括在
 * 回调内直接击杀或施加击退的附属模组回调。其他模组的提前锁定阶段与本阶段之间没有顺序约束，但它们都只返回 FAIL 或
 * PASS，因此无害。不受限的玩家始终得到 PASS，其他监听器之间的相对顺序不变。空手使用方块（门、按钮）与空手交互
 * 非玩家实体（座位）仍然放行；所有实体攻击都被拒绝，以此保证"无击退"。
 */
public final class AntiFastRoundGuards {
    public static final Identifier SAFE_TIME_PHASE = SparkFactionApiMod.id("anti_fast_round");
    private static boolean registered;

    private AntiFastRoundGuards() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        UseItemCallback.EVENT.addPhaseOrdering(SAFE_TIME_PHASE, Event.DEFAULT_PHASE);
        UseItemCallback.EVENT.register(SAFE_TIME_PHASE, (player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            return deny(player) ? TypedActionResult.fail(stack) : TypedActionResult.pass(stack);
        });
        // Wathe doors read the held key / lockpick / poison vial / scorpion inside the block's onUse, so denying
        // with-item block use stops those while bare-hand doors and buttons keep working.
        // Wathe 的门在方块 onUse 中读取手持的钥匙、撬锁器、毒药瓶、蝎子，因此拒绝持物使用方块即可拦截它们，空手开门、
        // 按按钮仍然可用。
        UseBlockCallback.EVENT.addPhaseOrdering(SAFE_TIME_PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(SAFE_TIME_PHASE, (player, world, hand, hit) ->
                !player.getStackInHand(hand).isEmpty() && deny(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.addPhaseOrdering(SAFE_TIME_PHASE, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.register(SAFE_TIME_PHASE, (player, world, hand, entity, hit) ->
                (!player.getStackInHand(hand).isEmpty() || entity instanceof PlayerEntity) && deny(player)
                        ? ActionResult.FAIL
                        : ActionResult.PASS);
        AttackEntityCallback.EVENT.addPhaseOrdering(SAFE_TIME_PHASE, Event.DEFAULT_PHASE);
        AttackEntityCallback.EVENT.register(SAFE_TIME_PHASE, (player, world, hand, entity, hit) ->
                deny(player) ? ActionResult.FAIL : ActionResult.PASS);
    }

    /** True (and the refusal is shown) only when the interaction is actually denied. / 仅在确实拒绝时返回 true 并提示。 */
    private static boolean deny(PlayerEntity player) {
        if (!AntiFastRound.isRestricted(player)) {
            return false;
        }
        AntiFastRound.notifyBlocked(player);
        return true;
    }
}
