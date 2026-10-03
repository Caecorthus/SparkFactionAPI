package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.ReplayTooltipContributor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registration-ordered tooltip contributors; re-registering an id keeps its original position.
 * 按注册顺序保存的悬停提示贡献者；重复注册同一 id 保留原位置。
 */
public final class ReplayTooltipContributors {
    private static final Map<Identifier, ReplayTooltipContributor> CONTRIBUTORS = new LinkedHashMap<>();

    private ReplayTooltipContributors() {
    }

    public static synchronized void register(Identifier id, ReplayTooltipContributor contributor) {
        CONTRIBUTORS.put(id, contributor);
    }

    public static synchronized List<Map.Entry<Identifier, ReplayTooltipContributor>> entries() {
        return List.copyOf(new ArrayList<>(CONTRIBUTORS.entrySet()));
    }
}
