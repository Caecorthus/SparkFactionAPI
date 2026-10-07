package dev.caecorthus.sparkfactionapi.impl.cooldown;

import dev.caecorthus.sparkfactionapi.mixin.cooldown.ItemCooldownEntryAccessor;
import dev.caecorthus.sparkfactionapi.mixin.cooldown.ItemCooldownManagerAccessor;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Exact vanilla item-cooldown reads and writes for forced cooldowns. Duration modifiers hook
 * {@code ItemCooldownManager.set} (SparkTraits Fast Hands x0.9, NoellesRoles Stimulation x0.8, ...), so a penalty
 * written through {@code set} alone would shrink. Instead the live entry is rewritten in place
 * (start = now, end = now + ticks); when no entry exists, {@code set} only creates one and is then corrected the
 * same way. The client gets an explicit exact packet: those modifiers are server-gated, so the client
 * {@code set} applies the packet duration unchanged.
 * 强制冷却的精确原版物品冷却读写。时长倍率挂在 {@code ItemCooldownManager.set} 上（SparkTraits 快手 ×0.9、
 * NoellesRoles 亢奋 ×0.8 等），仅靠 {@code set} 写入的惩罚会被缩短。因此直接原地改写现有条目（起点 = 当前，
 * 终点 = 当前 + ticks）；若无条目，只借 {@code set} 创建条目再同样校正。随后向客户端发送精确的数据包：这些倍率仅在
 * 服务端生效，客户端的 {@code set} 会原样采用包内时长。
 */
final class ExactItemCooldownWriter {
    private ExactItemCooldownWriter() {
    }

    static int remainingTicks(ItemCooldownManager manager, Item item) {
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        Object entry = access.sparkfactionapi$getEntries().get(item);
        return entry instanceof ItemCooldownEntryAccessor bounds
                ? ForcedCooldownRules.remaining(bounds.sparkfactionapi$getEndTick(), access.sparkfactionapi$getTick())
                : 0;
    }

    /**
     * The live vanilla entry for the item, or {@code null}. Its identity changes on every {@code set} and
     * {@code remove}; {@code ForcedItemLedger} keys on it.
     * 物品当前的原版条目，无则为 {@code null}。每次 {@code set} 与 {@code remove} 都会换成新对象；ForcedItemLedger 以其为键。
     */
    static ItemCooldownEntryAccessor liveEntry(ItemCooldownManager manager, Item item) {
        Object entry = ((ItemCooldownManagerAccessor) manager).sparkfactionapi$getEntries().get(item);
        return entry instanceof ItemCooldownEntryAccessor bounds ? bounds : null;
    }

    /** The manager's own tick counter (the time base of its entries). / 冷却管理器自身的刻计数（其条目的时间基准）。 */
    static int tick(ItemCooldownManager manager) {
        return ((ItemCooldownManagerAccessor) manager).sparkfactionapi$getTick();
    }

    /**
     * Installs exactly {@code ticks} remaining (callers pass a positive, already-monotonic value). Returns false only
     * when another mod cancelled {@code set} and no entry could be created.
     * 精确写入 {@code ticks} 剩余（调用方传入已满足单调性的正数）。仅当其他模组取消了 {@code set} 导致无法创建条目时返回 false。
     */
    static boolean writeExact(ServerPlayerEntity player, Item item, int ticks) {
        ItemCooldownManager manager = player.getItemCooldownManager();
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        Object entry = access.sparkfactionapi$getEntries().get(item);
        if (entry == null) {
            // Vanilla path only to create the entry (and fire set hooks); its possibly modified length is fixed below.
            // 仅借原版路径创建条目（并触发 set 钩子）；其可能被倍率修改的时长在下方校正。
            manager.set(item, ticks);
            entry = access.sparkfactionapi$getEntries().get(item);
        }
        if (!(entry instanceof ItemCooldownEntryAccessor bounds)) {
            return false;
        }
        int tick = access.sparkfactionapi$getTick();
        int endTick = ForcedCooldownRules.saturatingAdd(tick, ticks);
        bounds.sparkfactionapi$setStartTick(tick);
        bounds.sparkfactionapi$setEndTick(endTick);
        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(new CooldownUpdateS2CPacket(item, ForcedCooldownRules.remaining(endTick, tick)));
        }
        return true;
    }
}
