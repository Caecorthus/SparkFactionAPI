package dev.caecorthus.sparkfactionapi.impl.registry;

import dev.caecorthus.sparkfactionapi.api.FactionCapabilities;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.impl.nativefaction.NativeFactionRules;
import dev.caecorthus.sparkfactionapi.impl.roundend.FactionWinRules;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns faction definitions and capability lookup.
 * 持有阵营定义与能力查询。
 */
public final class FactionCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkFactionAPI/FactionCatalog");
    private static final Map<Identifier, FactionDefinition> FACTIONS = new LinkedHashMap<>();
    // FactionCapabilities is an immutable record (Set.copyOf tags), so one shared fallback is safe.
    // FactionCapabilities 是不可变 record（标签经 Set.copyOf），因此可以共享同一个缺省实例。
    private static final FactionCapabilities NO_CAPABILITIES = FactionCapabilities.none();
    // Win-check order of custom factions; registerFaction resets it to null. 自定义阵营的胜利检查顺序；registerFaction 会将其重置为 null。
    private static volatile List<FactionDefinition> customFactionsByPriority;

    private FactionCatalog() {
    }

    static void registerLegacyFaction(Identifier id, int color) {
        FactionCapabilities capabilities = switch (id.getPath()) {
            case "civilian" -> FactionCapabilities.builder()
                    .isPunishableInnocentGunVictim(true)
                    .isPunishableInnocentGunShooter(true)
                    .build();
            case "killer" -> FactionCapabilities.builder()
                    .canUseKillerFeatures(true)
                    .receivesKillerPassiveMoney(true)
                    .receivesKillRewards(true)
                    .hasBlackoutImmunity(true)
                    .sharesCohort(true)
                    .canUseInstinct(true)
                    .instinctColor(0x990000)
                    .build();
            default -> FactionCapabilities.none();
        };
        FACTIONS.put(id, FactionDefinition.builder(id)
                .color(color)
                .translationKeyPrefix("faction." + id.getNamespace() + "." + id.getPath())
                .capabilities(capabilities)
                .build());
    }

    public static FactionDefinition registerFaction(FactionDefinition definition) {
        FactionRegistryBootstrap.bootstrap();
        if (isLegacyFaction(definition.id())) {
            throw new IllegalArgumentException("Legacy wathe factions cannot be replaced: " + definition.id());
        }
        if (FACTIONS.containsKey(definition.id())) {
            throw new IllegalArgumentException("Faction already registered: " + definition.id());
        }
        // dealtInWatheNeutralPool off a native faction throws; neutralAligned there is only meaningless.
        // 非原生阵营设置 dealtInWatheNeutralPool 直接抛出；设置 neutralAligned 只是无意义。
        if (NativeFactionRules.validateNativeFlags(definition)) {
            LOGGER.warn("Faction {} sets neutralAligned(true) but is not a native Wathe faction; the flag is ignored. "
                    + "阵营 {} 不是原生 Wathe 阵营，neutralAligned 被忽略。", definition.id(), definition.id());
        }
        FACTIONS.put(definition.id(), definition);
        customFactionsByPriority = null;
        return definition;
    }

    public static FactionCapabilities capabilities(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        FactionDefinition definition = FACTIONS.get(factionId);
        // FactionDefinition's constructor already replaces null capabilities with none().
        // FactionDefinition 构造器已把 null 能力替换为 none()。
        return definition == null ? NO_CAPABILITIES : definition.capabilities();
    }

    public static Optional<FactionDefinition> getFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return Optional.ofNullable(FACTIONS.get(factionId));
    }

    /**
     * Allocation-free form of {@link #getFaction} for per-tick callers; null when the id is not registered.
     * 供每 tick 调用方使用的无分配版本 {@link #getFaction}；未注册时返回 null。
     */
    public static @Nullable FactionDefinition definitionOrNull(@Nullable Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return factionId == null ? null : FACTIONS.get(factionId);
    }

    public static Collection<FactionDefinition> getCustomFactions() {
        FactionRegistryBootstrap.bootstrap();
        return FACTIONS.values().stream()
                .filter(faction -> !isLegacyFaction(faction.id()))
                .toList();
    }

    /**
     * Custom factions in win-check order, cached for the per-tick win hook and rebuilt after a registration.
     * Each rebuild is a new unmodifiable list, so a caller's earlier list never changes.
     * 按胜利检查顺序排列的自定义阵营，为每 tick 的胜利钩子缓存，注册后重建。每次重建都是新的不可变列表，
     * 调用方先前拿到的列表不会改变。
     */
    public static List<FactionDefinition> customFactionsByPriority() {
        FactionRegistryBootstrap.bootstrap();
        List<FactionDefinition> cached = customFactionsByPriority;
        if (cached == null) {
            cached = FactionWinRules.priorityOrder(getCustomFactions());
            customFactionsByPriority = cached;
        }
        return cached;
    }

    public static boolean isCustomFaction(Identifier factionId) {
        FactionRegistryBootstrap.bootstrap();
        return FACTIONS.containsKey(factionId) && !isLegacyFaction(factionId);
    }

    static boolean containsRegisteredFaction(Identifier factionId) {
        return FACTIONS.containsKey(factionId);
    }

    private static boolean isLegacyFaction(Identifier id) {
        return FactionIds.NONE.equals(id)
                || FactionIds.CIVILIAN.equals(id)
                || FactionIds.KILLER.equals(id)
                || FactionIds.NEUTRAL.equals(id);
    }
}
