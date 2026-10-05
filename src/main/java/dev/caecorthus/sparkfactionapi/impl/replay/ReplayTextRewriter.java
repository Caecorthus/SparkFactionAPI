package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.caecorthus.sparkfactionapi.net.replay.ReplayTextTags;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Turns a chat replay line into its screen form without touching the original. A span whose style belongs to a
 * player loses its hover and gets a {@link ReplayTextTags} insertion instead; every other style, including unrelated
 * hovers such as item names, is kept. The copy is deep: siblings and the {@code Text} arguments of translatable
 * content are rebuilt.
 * 把聊天回放行改写为界面用形式，不修改原对象。属于某名玩家的片段去掉悬停并改为 {@link ReplayTextTags} 插入标记；
 * 其他样式（包括物品名等无关悬停）全部保留。复制为深拷贝：同级片段与可翻译内容中的 {@code Text} 参数都会重建。
 */
public final class ReplayTextRewriter {
    private ReplayTextRewriter() {
    }

    /**
     * Owner lookup for session-rendered names: their SHOW_TEXT hover value is the very tooltip instance the session
     * froze, so matching is by identity ({@code ==}) through an identity map.
     * 会话渲染名字的所属查找：其 SHOW_TEXT 悬停值正是会话冻结的那份提示实例，因此通过同一引用映射按 {@code ==} 匹配。
     *
     * @param identityOwners tooltip instance to player, keyed by identity; 提示实例到玩家的映射，须按同一引用作键
     */
    public static Function<Style, @Nullable UUID> byFrozenTooltip(Map<Text, UUID> identityOwners) {
        return style -> {
            HoverEvent hover = style.getHoverEvent();
            if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) {
                return null;
            }
            return identityOwners.get(hover.getValue(HoverEvent.Action.SHOW_TEXT));
        };
    }

    /**
     * @param ownerOf   player a span's style belongs to, or null; 片段样式所属的玩家，无则为 null
     * @param mentioned receives each player whose span was retagged, in text order; 按文本顺序接收每个被重新标记的玩家
     */
    public static MutableText rewrite(Text text, Function<Style, @Nullable UUID> ownerOf, Consumer<UUID> mentioned) {
        MutableText copy = MutableText.of(rewriteContent(text.getContent(), ownerOf, mentioned));
        copy.setStyle(rewriteStyle(text.getStyle(), ownerOf, mentioned));
        for (Text sibling : text.getSiblings()) {
            copy.append(rewrite(sibling, ownerOf, mentioned));
        }
        return copy;
    }

    private static TextContent rewriteContent(
            TextContent content,
            Function<Style, @Nullable UUID> ownerOf,
            Consumer<UUID> mentioned
    ) {
        if (!(content instanceof TranslatableTextContent translatable)) {
            // Other content types are immutable values; sharing them keeps the copy independent.
            // 其他内容类型是不可变值，共享它们不影响副本的独立性。
            return content;
        }
        Object[] args = translatable.getArgs();
        Object[] copied = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            copied[i] = args[i] instanceof Text arg ? rewrite(arg, ownerOf, mentioned) : args[i];
        }
        return new TranslatableTextContent(translatable.getKey(), translatable.getFallback(), copied);
    }

    private static Style rewriteStyle(Style style, Function<Style, @Nullable UUID> ownerOf, Consumer<UUID> mentioned) {
        UUID owner = ownerOf.apply(style);
        if (owner == null) {
            return style;
        }
        mentioned.accept(owner);
        return style.withHoverEvent(null).withInsertion(ReplayTextTags.tag(owner));
    }
}
