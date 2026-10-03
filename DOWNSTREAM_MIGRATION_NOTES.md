# Downstream Migration Notes

## 2026-10-03 Replay API (0.1.5.12)

Additive and source/binary compatible. No migration is required.

- New package `api/replay`: `SparkReplayApi.withRoleChangeCause(...)` tags a
  mid-round `GameWorldComponent#addRole` with a replay cause and optional source
  player; `registerPlayerTooltipContributor(...)` appends lines to each
  participant's replay hover tooltip.
- Every mid-round role change is now recorded as `sparkfactionapi:role_changed`
  and printed in the Wathe replay. Downstream formatters that call
  `ReplayGenerator.formatPlayerName` automatically show the role held at the
  time of their event and gain a hover tooltip; no code change is needed.
- Mods that already print their own conversion line may want to drop it to
  avoid duplicates (NoellesRoles `shadow_transform` is handled here).


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
