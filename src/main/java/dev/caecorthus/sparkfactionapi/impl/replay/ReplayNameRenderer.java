package dev.caecorthus.sparkfactionapi.impl.replay;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.record.GameRecordEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared replay text helpers. While a replay is being generated they resolve names against the active session
 * (time-aware role plus hover tooltip); outside generation they degrade to plain fallbacks.
 * 回放共享文本工具。生成回放期间按当前会话解析（按时间的身份与悬停提示）；生成之外退化为普通回退。
 */
public final class ReplayNameRenderer {
    static final int UNKNOWN_ROLE_COLOR = 0xFFFFFF;
    private static final String ROLE_KEY_PREFIX = "announcement.role.";
    private static final String NO_ROLE_KEY = "replay.sparkfactionapi.role.none";

    private ReplayNameRenderer() {
    }

    /** "name(role)" using the role held at the current event, with hover. 使用当前事件时刻身份的“名字(身份)”，带悬停。 */
    public static Text nameWithRole(UUID uuid) {
        ReplaySession session = ReplayRenderContext.session();
        if (session == null) {
            return uuidPrefix(uuid);
        }
        GameRecordEvent event = ReplayRenderContext.currentEvent();
        return session.nameWithRole(uuid, event == null ? null : event.seq());
    }

    /** Name only, coloured by final role, with hover. 仅名字，按最终身份着色，带悬停。 */
    public static Text plainName(UUID uuid) {
        ReplaySession session = ReplayRenderContext.session();
        return session == null ? uuidPrefix(uuid) : session.plainName(uuid);
    }

    /** Translated role label in the role colour; null renders the "no role" label. 按身份颜色显示的身份名；null 显示“无身份”。 */
    public static Text roleName(@Nullable Identifier roleId) {
        if (roleId == null) {
            return Text.translatable(NO_ROLE_KEY).formatted(Formatting.GRAY);
        }
        return roleLabel(roleId).withColor(roleColor(roleId));
    }

    static MutableText renderNameWithRole(String name, @Nullable Identifier roleId, @Nullable Text tooltip) {
        // Same shape as Wathe formatPlayerName: one colour for "name(role)". 与 Wathe formatPlayerName 一致：整体单一颜色。
        MutableText text = Text.literal(name + "(")
                .append(roleId == null ? Text.translatable(NO_ROLE_KEY) : roleLabel(roleId))
                .append(Text.literal(")"));
        return text.setStyle(nameStyle(roleColor(roleId), tooltip));
    }

    static MutableText renderPlainName(String name, @Nullable Identifier finalRoleId, @Nullable Text tooltip) {
        return Text.literal(name).setStyle(nameStyle(roleColor(finalRoleId), tooltip));
    }

    static int roleColor(@Nullable Identifier roleId) {
        Role role = roleId == null ? null : WatheRoles.getRole(roleId);
        return role != null ? role.color() : UNKNOWN_ROLE_COLOR;
    }

    static MutableText uuidPrefix(UUID uuid) {
        return Text.literal(uuid.toString().substring(0, 8));
    }

    private static MutableText roleLabel(Identifier roleId) {
        // Wathe's own key convention, readable path when a mod ships no translation. 沿用 Wathe 键约定，缺翻译时显示路径。
        return Text.translatableWithFallback(ROLE_KEY_PREFIX + roleId.getPath(), roleId.getPath());
    }

    private static Style nameStyle(int color, @Nullable Text tooltip) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(color));
        return tooltip == null ? style : style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
    }
}
