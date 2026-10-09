package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.SparkFactionApiMod;
import dev.doctor4t.wathe.item.CocktailItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Records {@code sparkfactionapi:consume} (actor = consumer, {@code item} id, {@code kind} food/drink) through two
 * server-side funnels:
 * <ul>
 *   <li>{@code ItemStack#finishUsing}: every finished eat/drink use (vanilla {@code consumeItem}), including Wathe
 *   cocktails, Noelle's fine drink / base spirit (which override {@code finishUsing} without {@code super}) and
 *   SparkStrength's Blue Belladonna eaten by hand. The item is read before the call, because finishing may empty the
 *   stack, and recorded after it returns, only if the stack was used or replaced (a refusal such as SparkStrength's
 *   Coroner cancelling a base spirit returns it untouched).</li>
 *   <li>Wathe {@code PoisonUtils.applyFoodPoison(target, stack)} outside the target's own {@code finishUsing}: the
 *   "food consumed" hook that consumers without a use animation call for the fed player, i.e. Noelle's Waiter feeding
 *   and SparkStrength capsules. Inside {@code finishUsing} (Wathe's {@code eatFood} hook, SparkWitch's poison-apple
 *   cocktail hook) it is skipped, so nothing is counted twice.</li>
 * </ul>
 * Not covered: consumption that calls neither (a capsule-delivered Blue Belladonna, {@code applyEaten} only), instant
 * items that are neither food nor drink by these rules (SparkWitch Fisher fish, SparkStrength Professor serum), and
 * creative players whose stack {@code finishUsing} leaves untouched.
 * 通过两个服务端汇聚点记录 {@code sparkfactionapi:consume}（actor = 食用者，{@code item} 物品标识，{@code kind}
 * food/drink）：{@code ItemStack#finishUsing} 覆盖所有完成的吃/喝（原版 {@code consumeItem}），包括 Wathe 鸡尾酒、Noelle
 * 的精酿饮品/基酒（重写 {@code finishUsing} 且不调用 {@code super}）以及手动食用的 SparkStrength 蓝颠茄；物品在调用前
 * 读取（完成使用可能清空物品堆），在返回后且物品堆被消耗或替换时才记录（拒绝饮用，如 SparkStrength 验尸官取消基酒，
 * 会原样返回物品堆）。Wathe {@code PoisonUtils.applyFoodPoison(target, stack)} 若不在目标自身的
 * {@code finishUsing} 内，则是无使用动画的进食方式为被喂者调用的“已进食”钩子，即 Noelle 服务员喂食与 SparkStrength
 * 胶囊；在 {@code finishUsing} 内（Wathe {@code eatFood} 钩子、SparkWitch 毒苹果鸡尾酒钩子）则跳过，避免重复计数。
 * 未覆盖：两者都不调用的进食（胶囊投喂的蓝颠茄，只调用 {@code applyEaten}）、按上述规则既非食物也非饮品的即时物品
 * （SparkWitch 渔夫的鱼、SparkStrength 教授血清），以及 {@code finishUsing} 未改动物品堆的创造模式玩家。
 */
public final class ConsumeRecorder {
    public static final String EVENT_TYPE = "sparkfactionapi:consume";
    public static final String KEY_ITEM = "item";
    public static final String KEY_KIND = "kind";

    /**
     * Item, kind (null = not food or drink) and stack count read before {@code finishUsing}.
     * 完成使用前读取的物品、类别（null 表示既非食物也非饮品）与物品堆数量。
     */
    public record Use(String item, @Nullable ConsumeKind kind, int countBefore) {
    }

    private static final PlayerScopeStack.PerThread<Use> OPEN_USES = new PlayerScopeStack.PerThread<>();

    private ConsumeRecorder() {
    }

    /**
     * Opens a {@code finishUsing} scope for a server player during an active match; pair with {@link #endUse} in a
     * {@code finally}. Returns null when nothing was opened.
     * 对局进行中为服务端玩家打开 {@code finishUsing} 作用域；须在 {@code finally} 中配对调用 {@link #endUse}。未打开时
     * 返回 null。
     */
    public static PlayerScopeStack.@Nullable Frame<Use> beginUse(ItemStack stack, @Nullable World world, @Nullable LivingEntity user) {
        try {
            if (world == null || world.isClient() || !(user instanceof ServerPlayerEntity player)
                    || !GameRecordManager.hasActiveMatch()) {
                return null;
            }
            return OPEN_USES.push(player.getUuid(), new Use(itemId(stack), kindOf(stack), stack.getCount()));
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to read a finished item use for the match record", e);
            return null;
        }
    }

    /**
     * Closes the scope. Records only when {@code finishUsing} returned ({@code result} non-null) and really consumed
     * the item ({@link ConsumeRules#wasConsumed}).
     * 关闭作用域。仅当 {@code finishUsing} 已返回（{@code result} 非 null）且确实消耗了物品
     * （{@link ConsumeRules#wasConsumed}）时记录。
     */
    public static void endUse(
            PlayerScopeStack.@Nullable Frame<Use> frame,
            @Nullable LivingEntity user,
            ItemStack stack,
            @Nullable ItemStack result
    ) {
        if (frame == null) {
            return;
        }
        OPEN_USES.pop(frame);
        Use use = frame.value();
        if (result == null || use.kind() == null || !(user instanceof ServerPlayerEntity player)) {
            return;
        }
        if (ConsumeRules.wasConsumed(result != stack, use.countBefore(), stack.getCount())) {
            record(player, use);
        }
    }

    /**
     * Wathe {@code PoisonUtils.applyFoodPoison} HEAD: a fed or forced consumption when the target is not inside its
     * own {@code finishUsing}.
     * Wathe {@code PoisonUtils.applyFoodPoison} 头部：目标不在自身 {@code finishUsing} 内时，即为被喂食或强制进食。
     */
    public static void onFoodConsumed(@Nullable PlayerEntity target, @Nullable ItemStack stack) {
        try {
            if (!(target instanceof ServerPlayerEntity player) || stack == null || stack.isEmpty()
                    || player.getWorld().isClient() || !GameRecordManager.hasActiveMatch()) {
                return;
            }
            if (OPEN_USES.innermost(player.getUuid()) != null) {
                return;
            }
            Use use = new Use(itemId(stack), kindOf(stack), stack.getCount());
            if (use.kind() != null) {
                record(player, use);
            }
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record a fed consumption for the match record", e);
        }
    }

    private static void record(ServerPlayerEntity player, Use use) {
        try {
            GameRecordManager.event(EVENT_TYPE)
                    .actor(player)
                    .put(KEY_ITEM, use.item())
                    .put(KEY_KIND, use.kind().id())
                    .record();
        } catch (RuntimeException | LinkageError e) {
            SparkFactionApiMod.LOGGER.warn("Failed to record a consumption by {}", player.getUuid(), e);
        }
    }

    private static @Nullable ConsumeKind kindOf(ItemStack stack) {
        return ConsumeRules.classify(
                stack.getItem() instanceof CocktailItem,
                stack.getUseAction() == UseAction.DRINK,
                stack.get(DataComponentTypes.FOOD) != null
        );
    }

    private static String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
