package dev.caecorthus.sparkfactionapi.api.cooldown;

/**
 * The two families of cooldowns that {@link ForcedCooldowns} can force on a player.
 * {@link ForcedCooldowns} 可以强制施加给玩家的两类冷却。
 */
public enum CooldownKind {
    /** A role-skill counter owned by a registered {@link RoleSkillCooldownStore}. / 已注册存储持有的职业技能冷却。 */
    ROLE_SKILL,
    /** A vanilla per-item cooldown on a carried item. / 携带物品上的原版物品冷却。 */
    ITEM
}
