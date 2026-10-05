package dev.caecorthus.sparkfactionapi.api.replay;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A small coloured label shown on a participant's card in the replay screen (e.g. one trait).
 * Final class with a factory rather than a record so later releases can add fields compatibly.
 * 回放界面参与者卡片上的彩色小标签（例如一个词条）。使用带工厂方法的 final 类而非 record，便于后续兼容地追加字段。
 */
public final class ReplayBadge {
    private final Text label;
    private final int color;
    private final @Nullable Text tooltip;

    private ReplayBadge(Text label, int color, @Nullable Text tooltip) {
        this.label = label;
        this.color = color;
        this.tooltip = tooltip;
    }

    /**
     * @param color RGB colour of the label (alpha ignored); 标签的 RGB 颜色（忽略透明度）
     * @param tooltip shown when the badge is hovered, may be null; 悬停标签时显示的提示，可为 null
     */
    public static ReplayBadge of(Text label, int color, @Nullable Text tooltip) {
        return new ReplayBadge(Objects.requireNonNull(label, "label"), color & 0xFFFFFF, tooltip);
    }

    public Text label() {
        return label;
    }

    public int color() {
        return color;
    }

    public Optional<Text> tooltip() {
        return Optional.ofNullable(tooltip);
    }
}
