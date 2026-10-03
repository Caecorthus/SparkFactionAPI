package dev.caecorthus.sparkfactionapi.api.replay;

import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Appends lines to one participant's replay hover tooltip. Must be read-only: never mutate game state here.
 * Prefer translatable text so each client resolves its own language.
 * 为单名参与者的回放悬停提示追加内容。必须只读，不得修改游戏状态；优先使用可翻译文本，由客户端按语言解析。
 */
@FunctionalInterface
public interface ReplayTooltipContributor {
    void contribute(ReplayPlayerView player, Consumer<Text> lines);
}
