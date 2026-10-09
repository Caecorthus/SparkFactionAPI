package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Records {@code sparkfactionapi:income} (actor = player, {@code amount} int): the coins each player gained during
 * the match through Wathe {@code PlayerShopComponent#addToBalance} on the server, written once per player with a
 * positive total at the head of {@code GameRecordManager.endMatch}, while the match is still active and before
 * {@code match_end}. Each call counts its actual balance change (after − before), so other mods' changes to the
 * credited amount (SparkTraits' Snowball payout inside {@code addToBalance}) are included and deductions are not.
 * Starting money and every other {@code setBalance} write, purchases, Wathe's dev-environment auto-balance (writes the
 * field directly) and income credited somewhere other than the personal balance (SparkWitch's disguised Black Raven
 * wallet, the SparkStrength killer-team purse) are not income here.
 * 记录 {@code sparkfactionapi:income}（actor = 玩家，{@code amount} 整数）：每名玩家本局在服务端通过 Wathe
 * {@code PlayerShopComponent#addToBalance} 获得的金币，在 {@code GameRecordManager.endMatch} 头部（对局仍进行中、
 * {@code match_end} 之前）为每名累计值为正的玩家写一条。每次调用按实际余额变化（调用后 − 调用前）计入，因此其他模组对
 * 入账金额的改动（SparkTraits 雪球在 {@code addToBalance} 内的加成）会被计入，扣款不会。开局金钱及其他所有
 * {@code setBalance} 写入、购买、Wathe 开发环境自动补钱（直接写字段），以及记入个人余额以外位置的收入（SparkWitch
 * 伪装黑羽鸦的钱包、SparkStrength 杀手团队资金）都不算这里的收入。
 */
public final class IncomeRecorder {
    public static final String EVENT_TYPE = "sparkfactionapi:income";
    public static final String KEY_ACTOR = "actor";
    public static final String KEY_AMOUNT = "amount";

    private static final IncomeTotals TOTALS = new IncomeTotals();
    // Open addToBalance calls per credited player; the value is the balance before the call.
    // 按入账玩家记录进行中的 addToBalance 调用；值为调用前的余额。
    private static final PlayerScopeStack.PerThread<Integer> OPEN_CREDITS = new PlayerScopeStack.PerThread<>();

    private IncomeRecorder() {
    }

    /**
     * Opens an {@code addToBalance} scope for a server player during an active match; pair with {@link #endCredit}
     * in a {@code finally}. Returns null when nothing was opened, including for a call nested in an open call for the
     * same player, whose change the outer call already measures.
     * 对局进行中为服务端玩家打开 {@code addToBalance} 作用域；须在 {@code finally} 中配对调用 {@link #endCredit}。未打开
     * 时返回 null，包括嵌套在同一玩家进行中调用内的调用（其变化已由外层调用测得）。
     */
    public static PlayerScopeStack.@Nullable Frame<Integer> beginCredit(@Nullable PlayerEntity owner, int balanceBefore) {
        try {
            if (!(owner instanceof ServerPlayerEntity player) || !GameRecordManager.hasActiveMatch()
                    || OPEN_CREDITS.innermost(player.getUuid()) != null) {
                return null;
            }
            return OPEN_CREDITS.push(player.getUuid(), balanceBefore);
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to open a balance credit for the match record", e);
            return null;
        }
    }

    /** Closes the scope and adds the positive balance change to the match total. 关闭作用域，把正的余额变化计入本局累计。 */
    public static void endCredit(PlayerScopeStack.@Nullable Frame<Integer> frame, int balanceAfter) {
        if (frame == null) {
            return;
        }
        try {
            OPEN_CREDITS.pop(frame);
            long income = IncomeTotals.actualIncome(frame.value(), balanceAfter);
            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            if (income > 0 && match != null && GameRecordManager.hasActiveMatch()) {
                TOTALS.add(match.getMatchId(), frame.player(), income);
            }
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to count a balance credit of {} for the match record", frame.player(), e);
        }
    }

    /**
     * Head of {@code GameRecordManager.endMatch}: writes one event per player with income and forgets the totals.
     * {@code GameRecordManager.endMatch} 头部：为每名有收入的玩家写一条事件并清空累计值。
     */
    public static void onMatchEnd(@Nullable ServerWorld world) {
        try {
            GameRecordManager.MatchRecord match = GameRecordManager.getCurrentMatch();
            if (world == null || match == null || !GameRecordManager.hasActiveMatch()) {
                return;
            }
            for (Map.Entry<UUID, Integer> total : TOTALS.drain(match.getMatchId()).entrySet()) {
                record(world, total.getKey(), total.getValue());
            }
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record match income totals", e);
        }
    }

    /** Tail of {@code GameRecordManager.startMatch}: a new match starts with no totals. 新对局从零累计开始。 */
    public static void onMatchStart() {
        TOTALS.reset();
    }

    private static void record(ServerWorld world, UUID player, int amount) {
        try {
            GameRecordManager.EventBuilder event = GameRecordManager.event(EVENT_TYPE).world(world);
            ServerPlayerEntity online = world.getServer().getPlayerManager().getPlayer(player);
            if (online != null) {
                event.actor(online);
            } else {
                event.putUuid(KEY_ACTOR, player);
            }
            event.putInt(KEY_AMOUNT, amount).record();
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record the match income of {}", player, e);
        }
    }
}
