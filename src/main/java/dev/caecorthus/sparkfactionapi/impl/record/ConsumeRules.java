package dev.caecorthus.sparkfactionapi.impl.record;

import org.jetbrains.annotations.Nullable;

/**
 * Pure decisions for {@code sparkfactionapi:consume}.
 * {@code sparkfactionapi:consume} 的纯判定规则。
 */
public final class ConsumeRules {
    private ConsumeRules() {
    }

    /**
     * A drink is a Wathe {@code CocktailItem} (including subclasses such as Noelle's fine drink and base spirit) or any
     * item whose use action is DRINK; otherwise an item with a food component is food. Drink wins, so a cocktail or a
     * vanilla honey bottle (food component, DRINK action) is a drink. Null: neither, not recorded.
     * 饮品为 Wathe {@code CocktailItem}（含 Noelle 的精酿饮品、基酒等子类）或使用动作为 DRINK 的物品；否则带食物组件的
     * 物品为食物。饮品优先，因此鸡尾酒与原版蜂蜜瓶（食物组件 + DRINK 动作）算饮品。返回 null 表示两者都不是，不记录。
     */
    public static @Nullable ConsumeKind classify(boolean cocktail, boolean drinkAction, boolean hasFood) {
        if (cocktail || drinkAction) {
            return ConsumeKind.DRINK;
        }
        return hasFood ? ConsumeKind.FOOD : null;
    }

    /**
     * Whether a returned {@code finishUsing} really consumed the item: it handed back another stack (bowl, bottle,
     * bucket) or used some of the stack. A refusal (SparkStrength's Coroner cancelling a base spirit) returns the same,
     * untouched stack. Creative players whose stack is not used up cannot be confirmed and are not counted.
     * 返回的 {@code finishUsing} 是否真的消耗了物品：返回了另一个物品堆（碗、瓶、桶）或物品堆数量减少。拒绝（如
     * SparkStrength 验尸官取消基酒）会原样返回未改动的同一物品堆。物品未被消耗的创造模式玩家无法确认，不计入。
     */
    public static boolean wasConsumed(boolean returnedAnotherStack, int countBefore, int countAfter) {
        return returnedAnotherStack || countAfter < countBefore;
    }
}
