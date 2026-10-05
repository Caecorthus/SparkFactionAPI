package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.caecorthus.sparkfactionapi.net.replay.ReplaySnapshot;
import dev.doctor4t.wathe.record.GameRecordTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Pure mapping from a Wathe record event type to the replay screen's timeline filter bucket. Add-on types that are
 * not listed (including Wathe {@code global_event}) fall into OTHER.
 * Wathe 记录事件类型到回放界面时间线筛选分类的纯映射；未列出的附属模组类型（包括 Wathe {@code global_event}）归入 OTHER。
 */
public final class ReplayLineCategories {
    // NoellesRoles custom record types (GameRecordManager.event(...)); optional dependency, so plain strings.
    // NoellesRoles 自定义记录类型；它是可选依赖，因此直接使用字符串。
    static final String NOELLES_VOODOO_CHAIN_DEATH = "voodoo_chain_death";
    static final String NOELLES_DEATH_IN_STOMACH = "death_in_stomach";
    static final String NOELLES_SHADOW_TRANSFORM = "shadow_transform";

    private ReplayLineCategories() {
    }

    public static ReplaySnapshot.Category of(@Nullable String eventType) {
        if (eventType == null) {
            return ReplaySnapshot.Category.OTHER;
        }
        return switch (eventType) {
            case GameRecordTypes.DEATH, NOELLES_VOODOO_CHAIN_DEATH, NOELLES_DEATH_IN_STOMACH ->
                    ReplaySnapshot.Category.DEATH;
            case SparkReplayApi.ROLE_CHANGED_EVENT_TYPE, NOELLES_SHADOW_TRANSFORM -> ReplaySnapshot.Category.CONVERSION;
            case GameRecordTypes.SKILL_USE -> ReplaySnapshot.Category.SKILL;
            case GameRecordTypes.SHOP_PURCHASE -> ReplaySnapshot.Category.SHOP;
            case GameRecordTypes.ITEM_USE, GameRecordTypes.ITEM_PICKUP, GameRecordTypes.PLATTER_TAKE,
                 GameRecordTypes.PLAYER_POISONED -> ReplaySnapshot.Category.ITEM;
            default -> ReplaySnapshot.Category.OTHER;
        };
    }
}
