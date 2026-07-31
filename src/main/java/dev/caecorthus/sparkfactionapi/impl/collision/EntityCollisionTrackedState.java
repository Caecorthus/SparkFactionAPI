package dev.caecorthus.sparkfactionapi.impl.collision;

/**
 * Internal bridge for the server-authoritative collision exemption tracked value.
 * 服务端权威碰撞豁免追踪值的内部桥接接口。
 */
public interface EntityCollisionTrackedState {
    boolean sparkfactionapi$isCollisionExempt();

    void sparkfactionapi$setCollisionExempt(boolean exempt);
}
