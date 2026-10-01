package dev.caecorthus.sparkfactionapi.api.cooldown;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable snapshot of one forcible cooldown a player has right now. For {@link CooldownKind#ROLE_SKILL}
 * the id is the store id; for {@link CooldownKind#ITEM} it is the item registry id. A snapshot is never
 * written back: {@link ForcedCooldowns#raise} and {@link ForcedCooldowns#extend} re-read the live value.
 * 玩家此刻可被强制的一项冷却的不可变快照。职业技能时 id 为存储 id；物品时为物品注册 id。快照不会被回写：
 * {@link ForcedCooldowns#raise} 与 {@link ForcedCooldowns#extend} 会重新读取实时剩余值。
 *
 * @param kind           cooldown family / 冷却类别
 * @param id             store id or item id / 存储 id 或物品 id
 * @param remainingTicks remaining ticks at snapshot time, never negative / 快照时的剩余 tick，非负
 * @param nominalTicks   the full (nominal) cooldown when known / 已知时为完整（标准）冷却
 */
public record CooldownSlot(CooldownKind kind, Identifier id, int remainingTicks, OptionalInt nominalTicks) {
    public CooldownSlot {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        remainingTicks = Math.max(0, remainingTicks);
        nominalTicks = nominalTicks == null ? OptionalInt.empty() : nominalTicks;
    }
}
