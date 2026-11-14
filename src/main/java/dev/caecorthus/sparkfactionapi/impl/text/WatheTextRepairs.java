package dev.caecorthus.sparkfactionapi.impl.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Repairs for Wathe text that renders wrongly, kept on SparkFactionAPI's side because Wathe is upstream.
 * 修正显示有误的 Wathe 文字；Wathe 是上游模组，所以修正放在 SparkFactionAPI 这边。
 */
public final class WatheTextRepairs {
    private static final String LETTER_PREFIX = "tip.letter.";
    private static final String NAME_SUFFIX = ".name";
    private static final String ROLE_KEY_PREFIX = "announcement.role.";

    private WatheTextRepairs() {
    }

    /**
     * Wathe resolves the letter's role name to a plain string on the server ({@code getString()}), so a dedicated
     * server, which only loads {@code en_us}, writes the English name into every player's letter. This turns the
     * role argument of the {@code tip.letter[.<role>|.<faction>].name} line back into a translatable text with the
     * same style, so each client shows it in its own language. Returns null when nothing changed.
     * Wathe 在服务端把信件里的身份名解析成纯文字（{@code getString()}）；专用服务器只加载 {@code en_us}，于是每个人的
     * 信件里都写着英文身份名。这里把 {@code tip.letter[.<身份>|.<阵营>].name} 那一行的身份参数换回样式相同的可翻译文本，
     * 让每个客户端按自己的语言显示。没有改动时返回 null。
     */
    public static @Nullable List<Text> translatableLetterRoleName(List<Text> lines, @Nullable String roleName) {
        if (roleName == null) {
            return null;
        }
        for (int index = 0; index < lines.size(); index++) {
            Text line = lines.get(index);
            if (!(line.getContent() instanceof TranslatableTextContent content)
                    || !content.getKey().startsWith(LETTER_PREFIX)
                    || !content.getKey().endsWith(NAME_SUFFIX)) {
                continue;
            }
            Object[] args = content.getArgs();
            if (args.length < 2 || !(args[1] instanceof Text role) || !(role.getContent() instanceof PlainTextContent)) {
                return null;
            }
            Object[] repaired = args.clone();
            MutableText roleText = Text.translatable(ROLE_KEY_PREFIX + roleName).setStyle(role.getStyle());
            role.getSiblings().forEach(roleText::append);
            repaired[1] = roleText;
            MutableText repairedLine = Text.translatable(content.getKey(), repaired).setStyle(line.getStyle());
            line.getSiblings().forEach(repairedLine::append);
            List<Text> result = new ArrayList<>(lines);
            result.set(index, repairedLine);
            return result;
        }
        return null;
    }
}
