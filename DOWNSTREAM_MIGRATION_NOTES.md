# Downstream Migration Notes

## 2026-10-09 Achievement Record Fields (unreleased)

Additive. No migration is required. Writers and readers follow the
achievement record contract; new event types have no replay formatter and
never appear in the replay.

- Wathe `death` records gain kill-start fields: `victim_role`,
  `victim_faction`, `victim_psycho`, and with a killer `killer_role`,
  `killer_faction`, `killer_psycho`, `killer_item`, `distance` (same world
  only). Roles are omitted when absent; factions are effective faction ids;
  booleans arrive as "1"/"0". Existing fields are unchanged.
- New `sparkfactionapi:psycho` (actor, `active` bool) on every real psycho
  start and end. Add-ons that start or stop psycho must keep going through
  `PlayerPsychoComponent.startPsycho/stopPsycho` (or fire
  `PsychoModeEvents` themselves, as SparkStrength's Serial Killer override
  does); writing `psychoTicks` alone from 0 is not recorded.
- New `sparkfactionapi:consume` (actor, `item`, `kind` `food`/`drink`) for every
  finished eat/drink use that used or replaced the stack, and for players fed
  without a use animation. An add-on that makes a player consume an item
  without `finishUsing` should call Wathe `PoisonUtils.applyFoodPoison(target,
  stack)` (as Noelle's Waiter and SparkStrength capsules do) to be recorded;
  an add-on that refuses a drink inside `finishUsing` should return the stack
  untouched.
- New `sparkfactionapi:income` (actor, `amount` int), one per player with
  income, written at the start of `GameRecordManager.endMatch` before
  `match_end`: the match total of positive actual balance changes made by
  `PlayerShopComponent#addToBalance` on the server. Add-ons that pay players
  should keep crediting through `addToBalance` to be counted; `setBalance`
  writes (starting money, transfers, corrections) and direct `balance` field
  writes are not income. An offline player's `actor` is written with
  `putUuid`, so readers must not assume the actor is online.

## 2026-10-08 Match Record Payload (unreleased)

Additive. No migration is required.

- New S2C payload `sparkfactionapi:match_record`. When Wathe ends a match
  record, every client that registered it gets the match's structured events
  (Wathe record order, no `door_interaction`, earliest 8192 within 900 KiB,
  `tick` since the match start, Wathe NBT unchanged). It arrives before the
  round-end announcement and opens nothing; replay behavior is unchanged.
- Client read access: `dev.caecorthus.sparkfactionapi.client.api.SparkMatchRecordClient.latest()`
  returns the latest `MatchRecordSnapshot` or null (cleared on disconnect).
  The class name, `latest()`, and the record accessors are a stable contract
  that SparkAssist reads by reflection.

## 2026-10-07 Clear Keeping Forced Cooldowns (unreleased)

Additive and source/binary compatible. No migration is required.

- New `ForcedCooldowns.clearItemKeepingForced(ServerPlayerEntity, Item)`
  returns `int`. Role mechanics that reset or refresh an item cooldown should
  call it instead of `ItemCooldownManager.remove`. It still removes the
  natural cooldown, and owner `remove` hooks still run, but a penalty, aura or
  debuff that another feature forced through `raise`/`extend`/`raiseAll` stays
  on the item for whatever of it is not yet served (an extension still keeps
  the ticks it added while the cooldown it was appended to is running).
- Item `raise`/`extend` behave exactly as before; they now also remember their
  forced part on the live vanilla entry. A plain `remove` (admin
  `clearCooldown`, Wathe reset) still clears everything, forced parts included.

## 2026-10-05 Replay Screen (0.1.5.15)

Additive and source/binary compatible. No migration is required.

- New `SparkReplayApi.registerPlayerBadgeContributor(...)` with `ReplayBadge`
  and `ReplayBadgeContributor`: small coloured labels on each participant's card
  in the new replay screen (same lifecycle as tooltip contributors).
- Players with SparkFactionAPI on the client now get a short end-of-match chat
  summary instead of the full chat replay; `/replay` opens the screen and
  `/replay chat` prints the full chat replay. Existing replay formatters need no
  change: their lines appear in the screen automatically.


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
