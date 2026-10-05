package dev.caecorthus.sparkfactionapi.api.replay;

import java.util.function.Consumer;

/**
 * Adds badges to one participant's card in the replay screen. Same lifecycle and rules as
 * {@link ReplayTooltipContributor}: server thread, once per participant, read-only, before Wathe resets players.
 * 为回放界面中单名参与者的卡片追加标签。生命周期与规则同 {@link ReplayTooltipContributor}：服务端线程、每名参与者一次、
 * 只读、早于 Wathe 重置玩家。
 */
@FunctionalInterface
public interface ReplayBadgeContributor {
    void contribute(ReplayPlayerView player, Consumer<ReplayBadge> badges);
}
