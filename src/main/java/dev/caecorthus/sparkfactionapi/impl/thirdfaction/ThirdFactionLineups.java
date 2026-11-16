package dev.caecorthus.sparkfactionapi.impl.thirdfaction;

import dev.caecorthus.sparkfactionapi.api.FactionAssignmentPolicy;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.ThirdFactionLineup;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactionDeclarations;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeWatheFactions;
import dev.caecorthus.sparkfactionapi.impl.registry.FactionCatalog;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registered third-faction lineups, keyed by faction id. Registration is common-init only and copy-on-write, so the
 * server thread reads an immutable snapshot without locking.
 * 已注册的第三方阵营阵容，按阵营 id 索引。只在通用初始化期间注册，采用写时复制，服务端线程无需加锁即可读取不可变快照。
 */
public final class ThirdFactionLineups {
    private static volatile Map<Identifier, ThirdFactionLineup> lineups = Map.of();

    private ThirdFactionLineups() {
    }

    /**
     * Validates and stores a lineup. A third faction must be a registered, declared native faction without
     * {@code dealtInWatheNeutralPool} and without an assignment policy: neither Wathe's neutral pass nor an SFA
     * assignment phase may be a second natural draw path for it.
     * 校验并保存阵容。第三方阵营必须是已注册、已声明的原生阵营，且未启用 {@code dealtInWatheNeutralPool}、没有分配策略：
     * Wathe 中立分配与 SFA 分配阶段都不能成为它的第二条自然抽取途径。
     *
     * @throws IllegalArgumentException not a registered declared native faction, dealt in Wathe's neutral pool, or
     *                                  given an assignment policy
     * @throws IllegalStateException    a lineup is already registered for this faction
     */
    public static synchronized void register(Identifier factionId, ThirdFactionLineup lineup) {
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(lineup, "lineup");
        if (!NativeWatheFactions.isNativeCustomFaction(factionId)) {
            throw new IllegalArgumentException("A third-faction lineup needs a native Wathe faction declared under "
                    + NativeWatheFactionDeclarations.METADATA_KEY + " in fabric.mod.json: " + factionId);
        }
        FactionDefinition definition = FactionCatalog.definitionOrNull(factionId);
        if (definition == null) {
            throw new IllegalArgumentException("Register the faction with SparkFactionApi.registerFaction before its "
                    + "third-faction lineup: " + factionId);
        }
        if (definition.dealtInWatheNeutralPool()) {
            throw new IllegalArgumentException("A third faction cannot also be dealt in Wathe's neutral pool "
                    + "(dealtInWatheNeutralPool(true)): " + factionId);
        }
        // The SFA assignment phases would be a second draw path that ignores the cap and the one-faction rule.
        // SFA 分配阶段会成为第二条抽取途径，且无视上限与“仅一个阵营”的规则。
        if (definition.assignmentPolicy() != FactionAssignmentPolicy.NONE) {
            throw new IllegalArgumentException("A third faction cannot also be dealt by a FactionAssignmentPolicy; "
                    + "leave assignmentPolicy unset: " + factionId);
        }
        if (lineups.containsKey(factionId)) {
            throw new IllegalStateException("A third-faction lineup is already registered for " + factionId);
        }
        Map<Identifier, ThirdFactionLineup> next = new LinkedHashMap<>(lineups);
        next.put(factionId, lineup);
        lineups = Collections.unmodifiableMap(next);
    }

    public static @Nullable ThirdFactionLineup lineupOrNull(@Nullable Identifier factionId) {
        return factionId == null ? null : lineups.get(factionId);
    }

    /**
     * Factions with a registered lineup, in declared (ordinal) order, not registration order.
     * 已注册阵容的阵营，按声明（序号）顺序而非注册顺序。
     */
    public static List<Identifier> factionsWithLineup() {
        Map<Identifier, ThirdFactionLineup> current = lineups;
        if (current.isEmpty()) {
            return List.of();
        }
        List<Identifier> result = new ArrayList<>(current.size());
        for (Identifier factionId : NativeWatheFactions.customFactionIds()) {
            if (current.containsKey(factionId)) {
                result.add(factionId);
            }
        }
        return result;
    }
}
