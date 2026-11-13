package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.ReplayBadgeContributor;
import dev.caecorthus.sparkfactionapi.api.replay.ReplayTooltipContributor;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registration-ordered replay contributors; re-registering an id keeps its original position. One instance per
 * contributor kind (tooltip lines, badges), each with its own map and lock, so the two never share entries.
 * 按注册顺序保存的回放贡献者；重复注册同一 id 保留原位置。每种贡献者（悬停提示、标签）各一个实例，各自持有映射与锁，
 * 两者互不共享条目。
 */
public final class ReplayContributors<C> {
    public static final ReplayContributors<ReplayTooltipContributor> TOOLTIPS = new ReplayContributors<>();
    public static final ReplayContributors<ReplayBadgeContributor> BADGES = new ReplayContributors<>();

    private final Map<Identifier, C> contributors = new LinkedHashMap<>();

    private ReplayContributors() {
    }

    public synchronized void register(Identifier id, C contributor) {
        contributors.put(id, contributor);
    }

    synchronized List<Map.Entry<Identifier, C>> entries() {
        return List.copyOf(contributors.entrySet());
    }
}
