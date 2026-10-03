package dev.caecorthus.sparkfactionapi.impl.replay;

/**
 * Single entry point that wires the replay module during mod initialization.
 * 回放模块在模组初始化时的统一接线入口。
 */
public final class SparkReplayBootstrap {
    private SparkReplayBootstrap() {
    }

    public static void register() {
        RoleChangeReplay.register();
        ReplayPresentation.register();
    }
}
