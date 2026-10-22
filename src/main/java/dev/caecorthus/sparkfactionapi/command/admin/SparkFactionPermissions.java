package dev.caecorthus.sparkfactionapi.command.admin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

/**
 * Permission nodes for SparkFactionAPI administrator tools.
 * SparkFactionAPI 管理工具使用的权限节点。
 */
public final class SparkFactionPermissions {
    public static final int DEFAULT_COMMAND_LEVEL = 2;
    public static final String COMMAND_ADMIN = "sparkfactionapi.command.admin";

    private SparkFactionPermissions() {
    }

    /**
     * Requirement shared by every administrator command: the admin node, falling back to op level 2 when no
     * permission provider decides.
     * 所有管理命令共用的权限要求：管理节点；没有权限提供者作出判断时回落到 2 级 OP。
     */
    public static Predicate<ServerCommandSource> admin() {
        return Permissions.require(COMMAND_ADMIN, DEFAULT_COMMAND_LEVEL);
    }
}
