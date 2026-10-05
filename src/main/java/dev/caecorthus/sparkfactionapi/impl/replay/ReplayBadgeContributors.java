package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.api.replay.ReplayBadgeContributor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registration-ordered badge contributors; re-registering an id keeps its original position.
 * 按注册顺序保存的标签贡献者；重复注册同一 id 保留原位置。
 */
public final class ReplayBadgeContributors {
    private static final Map<Identifier, ReplayBadgeContributor> CONTRIBUTORS = new LinkedHashMap<>();

    private ReplayBadgeContributors() {
    }

    public static synchronized void register(Identifier id, ReplayBadgeContributor contributor) {
        CONTRIBUTORS.put(id, contributor);
    }

    public static synchronized List<Map.Entry<Identifier, ReplayBadgeContributor>> entries() {
        return List.copyOf(new ArrayList<>(CONTRIBUTORS.entrySet()));
    }
}
