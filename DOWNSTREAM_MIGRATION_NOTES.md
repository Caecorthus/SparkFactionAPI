# Downstream Migration Notes

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
