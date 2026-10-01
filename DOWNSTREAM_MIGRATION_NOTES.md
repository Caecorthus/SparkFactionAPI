# Downstream Migration Notes

## 2026-10-01 Forced Cooldown Registry (0.1.5.11)

This change is additive and source- and binary-compatible. No immediate
downstream migration is required.

New package `api/cooldown` (`ForcedCooldowns`, `RoleSkillCooldownStore`,
`CooldownSlot`, `CooldownKind`, `ItemCooldownNominalProvider`) gives features
that force cooldowns on another player one monotonic, exact path for role
skills and carried items. Integrations that own a role-skill counter should
register a `RoleSkillCooldownStore` during initialization (ids must be unique);
features that impose penalties should call `ForcedCooldowns.raiseAll` or
`slots` plus `raise`/`extend` on the server thread after
`SparkFactionApi.canAffectPlayer`. Calls with a player off the server thread
throw `IllegalStateException`.

Item writes bypass `ItemCooldownManager.set` duration modifiers and send an
exact `CooldownUpdateS2CPacket`. `noellesroles:timed_bomb` is exempt by
default. SparkFactionAPI adds two accessor mixins on `ItemCooldownManager` and
`ItemCooldownManager$Entry` with `sparkfactionapi$`-prefixed members. No policy
ordering, capability fallback, packet id, payload, component id, NBT key, or
round-end behavior changed.

## 2026-07-09 Role-Only Faction Lookup Clarification

This change is source- and binary-compatible. No immediate downstream migration
is required.

`SparkFactionApi.resolveEffectiveFaction(Role)` remains available, but it is now
deprecated because a `Role` cannot supply the player context needed by effective
faction resolvers. The method continues to return the same base-faction result.

Downstream code should use:

- `resolveBaseFaction(Role)` when only a role is available.
- `resolveEffectiveFaction(PlayerEntity, GameWorldComponent)` when temporary or
  player-specific faction changes must be observed.

No policy ordering, capability fallback, packet id, payload, component id, NBT
key, or round-end behavior changed.
