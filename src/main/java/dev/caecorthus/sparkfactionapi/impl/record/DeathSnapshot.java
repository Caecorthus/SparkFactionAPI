package dev.caecorthus.sparkfactionapi.impl.record;

import dev.caecorthus.sparkfactionapi.impl.replay.RoleChangeRecordRules;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Victim and killer state captured at the start of {@code GameFunctions.killPlayer}, written as extra fields onto the
 * Wathe {@code death} record (achievement record contract A1). Field names are a stable contract read by SparkAssist.
 * 在 {@code GameFunctions.killPlayer} 开始时捕获的受害者与凶手状态，作为额外字段写入 Wathe {@code death} 记录（成就
 * 记录契约 A1）。字段名是 SparkAssist 读取的稳定契约。
 *
 * @param victimRole  role id, null when the victim has no role; 身份标识，无身份时为 null
 * @param killer      null when no killer was passed; 未传入凶手时为 null
 */
public record DeathSnapshot(
        UUID victim,
        @Nullable String victimRole,
        String victimFaction,
        boolean victimPsycho,
        @Nullable Killer killer
) {
    public static final String KEY_VICTIM_ROLE = "victim_role";
    public static final String KEY_VICTIM_FACTION = "victim_faction";
    public static final String KEY_VICTIM_PSYCHO = "victim_psycho";
    public static final String KEY_KILLER_ROLE = "killer_role";
    public static final String KEY_KILLER_FACTION = "killer_faction";
    public static final String KEY_KILLER_PSYCHO = "killer_psycho";
    public static final String KEY_KILLER_ITEM = "killer_item";
    public static final String KEY_DISTANCE = "distance";

    /**
     * @param role     role id, null when the killer has no role; 身份标识，无身份时为 null
     * @param item     main-hand item id ({@code minecraft:air} when empty); 主手物品标识（空手为 {@code minecraft:air}）
     * @param distance feet-to-feet blocks, null when the killer is in another world; 脚到脚的格数，凶手在其他世界时为 null
     */
    public record Killer(
            UUID uuid,
            @Nullable String role,
            String faction,
            boolean psycho,
            String item,
            @Nullable Double distance
    ) {
        public Killer {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(faction, "faction");
            Objects.requireNonNull(item, "item");
        }
    }

    public DeathSnapshot {
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(victimFaction, "victimFaction");
    }

    /**
     * Writes the victim fields, and the killer fields only when {@code recordedKiller} is the killer captured at kill
     * start, so the extra fields never describe a different player than the record's {@code actor}.
     * 写入受害者字段；仅当 {@code recordedKiller} 与击杀开始时捕获的凶手相同时才写入凶手字段，保证额外字段描述的玩家
     * 与记录的 {@code actor} 一致。
     */
    public void writeTo(NbtCompound data, @Nullable UUID recordedKiller) {
        if (victimRole != null) {
            data.putString(KEY_VICTIM_ROLE, victimRole);
        }
        data.putString(KEY_VICTIM_FACTION, victimFaction);
        data.putBoolean(KEY_VICTIM_PSYCHO, victimPsycho);
        if (killer == null || !killer.uuid().equals(recordedKiller)) {
            return;
        }
        if (killer.role() != null) {
            data.putString(KEY_KILLER_ROLE, killer.role());
        }
        data.putString(KEY_KILLER_FACTION, killer.faction());
        data.putBoolean(KEY_KILLER_PSYCHO, killer.psycho());
        data.putString(KEY_KILLER_ITEM, killer.item());
        if (killer.distance() != null) {
            data.putDouble(KEY_DISTANCE, killer.distance());
        }
    }

    /**
     * Role id to record; absent and Wathe's placeholder {@code wathe:no_role} both mean "no role".
     * 需记录的身份标识；无身份与 Wathe 占位身份 {@code wathe:no_role} 都视为“无身份”。
     */
    public static @Nullable String roleId(@Nullable Identifier role) {
        return role == null || RoleChangeRecordRules.WATHE_NO_ROLE.equals(role) ? null : role.toString();
    }
}
