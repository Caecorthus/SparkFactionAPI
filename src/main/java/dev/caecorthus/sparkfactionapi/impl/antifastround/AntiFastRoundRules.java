package dev.caecorthus.sparkfactionapi.impl.antifastround;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
     * so no optional mod class is ever loaded. Mirrors SparkStrength's {@code TaotieHeadRules.DAZE_BLOCKED_PAYLOADS}
     * (itself SparkWitch's Control Expert stun list without {@code wathe:storebuy}): payload-fired item uses (knife,
     * gun, add-on weapons) and every known role skill. The shop, movement, exits and UI-only payloads stay allowed.
     * An unregistered id never matches. A new skill payload must be classified here deliberately.
     * 稳定契约：发送者处于安全时间内时在服务端丢弃的 C2S 数据包。只保存纯 id，从不加载可选模组的类。与 SparkStrength
     * 的 {@code TaotieHeadRules.DAZE_BLOCKED_PAYLOADS} 一致（即 SparkWitch 控场专家眩晕列表去掉 {@code wathe:storebuy}）：
     * 通过数据包触发的物品使用（刀、枪、附属武器）以及所有已知职业技能。商店、移动、退出与仅界面用途的数据包仍然放行。
     * 未注册的 id 永远不会命中。新增技能包必须在此有意识地归类。
     */
    public static final Set<Identifier> BLOCKED_PAYLOADS = Set.copyOf(List.of(
            Identifier.of("wathe", "knifestab"),
            Identifier.of("wathe", "gunshoot"),

            Identifier.of("noellesroles", "ability"),
            Identifier.of("noellesroles", "assassin_guess_role"),
            Identifier.of("noellesroles", "detective_investigate"),
            Identifier.of("noellesroles", "morph"),
            Identifier.of("noellesroles", "morph_corpse_toggle"),
            Identifier.of("noellesroles", "party_animal_buzz"),
            Identifier.of("noellesroles", "reporter_mark"),
            Identifier.of("noellesroles", "silencer_silence"),
            Identifier.of("noellesroles", "spirit_project"),
            Identifier.of("noellesroles", "swapper"),
            Identifier.of("noellesroles", "taotie_swallow"),
            Identifier.of("noellesroles", "vulture"),
            Identifier.of("noellesroles", "demon_hunter_shoot"),
            Identifier.of("noellesroles", "shadow_ally_request"),

            Identifier.of("sparkwitch", "use_skill"),
            Identifier.of("sparkwitch", "emma_factor"),
            Identifier.of("sparkwitch", "fire_death_ray"),
            Identifier.of("sparkwitch", "fire_potion_launcher"),
            Identifier.of("sparkwitch", "use_curser_ability"),
            Identifier.of("sparkwitch", "use_orthopedist_skill"),
            Identifier.of("sparkwitch", "use_saboteur_skill"),
            Identifier.of("sparkwitch", "throw_kidnapper_body"),
            Identifier.of("sparkwitch", "guardian"),
            Identifier.of("sparkwitch", "vendetta_knife_stab"),
            Identifier.of("sparkwitch", "swordfish_stab"),
            Identifier.of("sparkwitch", "open_judge_selection"),
            Identifier.of("sparkwitch", "confirm_judge_selection"),
            Identifier.of("sparkwitch", "request_prophecy"),
            Identifier.of("sparkwitch", "confirm_prophecy"),
            Identifier.of("sparkwitch", "submit_tarot_divination_selection"),
            Identifier.of("sparkwitch", "seeker_remote_open"),
            Identifier.of("sparkwitch", "seeker_car_swallow"),
            Identifier.of("sparkwitch", "seeker_car_recall"),
            Identifier.of("sparkwitch", "seeker_car_use"),
            Identifier.of("sparkwitch", "select_black_raven_disguise"),
            Identifier.of("sparkwitch", "use_blind_attune"),
            Identifier.of("sparkwitch", "rift_hop"),
            Identifier.of("sparkwitch", "rift_gate_close"),
            Identifier.of("sparkwitch", "use_fiend_dash"),
            Identifier.of("sparkwitch", "use_apprentice_purify"),
            Identifier.of("sparkwitch", "magician_ability"),

            Identifier.of("sparkstrength", "noisemaker_glow"),
            Identifier.of("sparkstrength", "phantom_backpack_invisibility"),
            Identifier.of("sparkstrength", "coroner_morph"),
            Identifier.of("sparkstrength", "professor_remote_feed"),
            Identifier.of("sparkstrength", "demon_hunter_sniff"),
            Identifier.of("sparkstrength", "vulture_super_curse"),
            Identifier.of("sparkstrength", "call_tablet_meeting"),
            Identifier.of("sparkstrength", "cast_tablet_vote"),
            Identifier.of("sparkstrength", "confirm_tablet_vote"),
            Identifier.of("sparkstrength", "approve_suspect_removal"),
            Identifier.of("sparkstrength", "select_criminologist_target"),
            Identifier.of("sparkstrength", "reporter_communication"),
            Identifier.of("sparkstrength", "timekeeper_watch_mode"),
            Identifier.of("sparkstrength", "drone_pilot_start"),
            Identifier.of("sparkstrength", "drone_pilot_action"),
            Identifier.of("sparkstrength", "taotie_head_fire")));

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
