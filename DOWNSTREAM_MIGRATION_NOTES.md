# Downstream Migration Notes

## 2026-10-03 Two-Row Limited Inventory (0.1.5.13)

No API change. Behavior that add-ons can observe while Wathe's limited
inventory is in use (alive, survival, `TrainWorldComponent#hasHud`; the HUD
flag defaults to on, so this includes the lobby):

- `LimitedInventoryScreen` also shows and clicks player-inventory slots 27-35
  as a second row above the hotbar. Its frame is a blue recolor of Wathe's
  own strip texture, generated in memory at resource load; the divider and the
  hotbar keep Wathe's gold, and no Wathe artwork is shipped. Main
  slots 9-26 stay hidden. Items that add-ons park in hidden storage from slot 9
  upward stay hidden while 9-26 has room; loops that scan 9..35 can reach 27.
  Players can now move items into, or out of, 27-35. Hotbar-only rules
  (SparkStrength tablet, SparkWitch bound items) still treat 27-35 as outside
  the hotbar, so a bound item parked there is a stray to their sweeps.
- While psycho mode runs, clicks on 27-35 are ignored on both sides, so the
  psycho bat cannot leave the hotbar.
- `PlayerInventory#getEmptySlot` prefers 0-8, then 27-35, then 9-26, so
  pickups, `giveItemStack`, `insertStack` and `offerOrDrop` fill the visible row
  before hidden storage. Nothing is lost or dropped that vanilla would have
  kept; merging into existing stacks is unchanged.
- A default `ShopEntry#onBuy` (no custom handler) whose hotbar is full now
  succeeds into the second row. Custom buy handlers and direct callers of the
  static `ShopEntry.insertStackInFreeSlot` remain hotbar-only.
- The item tooltip in that screen is one box (name, then description) at
  Wathe's description anchor below the strip; the name no longer floats above
  the strip, where the second row now sits.
- An inactive `ClickableWidget` covers the second row's 176x22 band, so
  per-frame widget-obstacle scans (SparkWitch/SparkTraits info cards, the
  SparkAssist guidebook) avoid it. Code that hard-codes the 176x32 strip should
  treat `(x, y - 22, 176, 54)` as the inventory block.

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
`ItemCooldownManager$Entry` with `sparkfactionapi$`-prefixed members. A store,
provider, or exemption that throws a `RuntimeException` or `LinkageError` (for
example a store compiled against a different NoellesRoles build) is isolated
and logged. The forced-cooldown registry itself changes no policy ordering,
capability fallback, packet id, payload, component id, NBT key, or round-end
behavior.

0.1.5.11 is also the first release built after two earlier
`update/version-2` commits, so it ships their behavior changes too:

- `e002fd2` retires `CriminologistAffectMixin`. The Criminologist faction guard
  is no longer provided by SparkFactionAPI; pair this release with a
  SparkStrength build that owns that guard (`update/version-2`), not with an
  older SparkStrength.
- `3029ddf` re-pins the NoellesRoles packet guards to the lambdas of the
  shipped NoellesRoles jar (sha256 `fcb0da…`, the one SparkWitch pins) at
  `require = 1`. The guards are now live, so `canAffectPlayer` policies (for
  example SparkWitch's Wraith isolation) now also cancel the Morphling, Swapper,
  Assassin, Reporter, Detective, Taotie, Shadow ally, Silencer and Party Animal
  ability packets. A different NoellesRoles jar with other lambda numbers fails
  to load this mixin.

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
