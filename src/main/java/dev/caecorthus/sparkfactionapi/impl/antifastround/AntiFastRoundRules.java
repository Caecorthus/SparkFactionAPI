package dev.caecorthus.sparkfactionapi.impl.antifastround;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Side-neutral rules of the anti-fast-round safe time: settings bounds, window arithmetic and the skill payload list.
 * 开局安全时间（防速通局）的两端通用规则：设置范围、窗口计算与技能数据包列表。
 */
public final class AntiFastRoundRules {
    public static final int DEFAULT_SECONDS = 10;
    public static final int MIN_SECONDS = 1;
    public static final int MAX_SECONDS = 600;
    public static final long NO_WINDOW = -1L;
    /**
     * Wathe keeps the screen black for about 3 s after initialization ({@code FADE_TIME + FADE_PAUSE}). The configured
     * seconds count from the end of this fade, and the start notice is sent then; the lock already holds during it.
     * Wathe 初始化后约 3 秒仍处于黑屏淡入（{@code FADE_TIME + FADE_PAUSE}）。设置的秒数从淡入结束时开始计算，
     * 开局提示也在此时发送；淡入期间锁已经生效。
     */
    public static final int FADE_IN_TICKS = 60;

    /**
     * Stable contract: C2S payloads dropped on the server while the sender is inside the safe time. Plain ids only,
     * so no optional mod class is ever loaded: payload-fired item uses (knife, gun, add-on weapons) and every known
     * role skill. The shop, movement, exits and UI-only payloads stay allowed. It is close to SparkStrength's
     * {@code TaotieHeadRules.DAZE_BLOCKED_PAYLOADS} but not equal: the safe time also allows Black Raven crow flight
     * and USEC attachment changes. (Since 2026-11-13 the Fisher's swordfish stab is on both lists, like the Wathe
     * knife, so it is no longer a difference.) An unregistered id never matches. A new skill payload must be classified
     * deliberately, as a complete row of the canonical table.
     * 稳定契约：发送者处于安全时间内时在服务端丢弃的 C2S 数据包。只保存纯 id，从不加载可选模组的类：通过数据包触发的
     * 物品使用（刀、枪、附属武器）以及所有已知职业技能。商店、移动、退出与仅界面用途的数据包仍然放行。它与
     * SparkStrength 的 {@code TaotieHeadRules.DAZE_BLOCKED_PAYLOADS} 相近但并不相同：安全时间还放行黑羽鸦乌鸦飞行与
     * USEC 配件更换。（自 2026-11-13 起钓鱼佬的剑鱼刺击与 Wathe 小刀一样同在两份名单中，不再是差异。）未注册的 id 永远不会命中。新增技能包必须作为规范表中完整的一行有意识地归类。
     */
    // Generated SAFE_TIME column of config/payload-policy/actions.tsv: edit that table and rerun
    // tools/payload_policy.py, never list ids here; per-id reasons are in its note column.
    // 生成的 SAFE_TIME 列（config/payload-policy/actions.tsv）：请修改该表并重新运行
    // tools/payload_policy.py，不要在此列出 id；各 id 的理由见其 note 列。
    public static final Set<Identifier> BLOCKED_PAYLOADS = GeneratedPayloadPolicy.SAFE_TIME;

    private AntiFastRoundRules() {
    }

    public static boolean isBlockedPayload(@Nullable Identifier payloadId) {
        return payloadId != null && BLOCKED_PAYLOADS.contains(payloadId);
    }

    public static int clampSeconds(int seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    /**
     * World time at which a window opened at {@code now} closes: the fade-in plus the configured seconds.
     * 在 {@code now} 开启的窗口的结束世界时间：淡入时长加上设置的秒数。
     */
    public static long windowEnd(long now, int seconds) {
        return now + FADE_IN_TICKS + clampSeconds(seconds) * 20L;
    }

    public static boolean isWithinWindow(long now, long windowEnd) {
        return windowEnd != NO_WINDOW && now < windowEnd;
    }

    /** Whole seconds left, rounded up; zero outside the window. / 剩余整秒数（向上取整）；窗口外为零。 */
    public static int remainingSeconds(long now, long windowEnd) {
        if (!isWithinWindow(now, windowEnd)) {
            return 0;
        }
        return (int) ((windowEnd - now + 19L) / 20L);
    }
}
