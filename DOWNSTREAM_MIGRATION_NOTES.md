# Downstream Migration Notes

## 2026-11-04 Native Wathe Factions (0.1.6.0, provisional)

Additive and source/binary compatible. No migration is required for factions
that do not opt in.

- A mod can turn custom factions into real Wathe `Faction` constants by adding
  to its `fabric.mod.json`:
  `"custom": {"sparkfactionapi:native_wathe_factions": {"sparkwitch:witch": "WITCH"}}`.
  The constants are appended after Wathe's four, sorted by faction id. A bad or
  conflicting declaration crashes startup with a message naming the mod.
- Roles registered under such a faction with the default NONE bucket now read
  as that constant in Wathe (`Role#getFaction`), not NEUTRAL, and
  `Role#isNeutral` is false for them. Code that gave these roles neutral perks
  through `isNeutral()` or `getFaction() == NEUTRAL` should call
  `SparkFactionApi.isNeutralAligned(role)` (true for NEUTRAL and for native
  factions built with `neutralAligned(true)`). Mods built against an older
  SparkFactionAPI should look the method up by reflection and fall back to
  `role.isNeutral()`.
- Any exhaustive `switch` over `Faction` without a `default` throws
  `MatchException` for the new constants. Add a default, or map the constant
  with `SparkFactionApi.factionIdOf(faction)`.
- Wathe stops counting these roles in its neutral pass: they no longer take a
  neutral slot and are never drawn there, unless the faction is built with
  `dealtInWatheNeutralPool(true)` (then exactly as before). Wathe's rotation
  history counts them as civilian rounds (`RoleHistoryComponent` uses
  `isNeutral`).
- Wathe `role_assigned` match records carry the constant's `name()`
  (`"WITCH"`). Map a record name with
  `SparkFactionApi.factionIdOf(Faction.valueOf(name))` and catch
  `IllegalArgumentException` for names this side does not declare.
- New: `SparkFactionApi.nativeWatheFaction(Identifier)`,
  `factionIdOf(Faction)`, `bindRoleFaction(Identifier roleId, Identifier factionId)`,
  `isNeutralAligned(Role)`; `FactionDefinition.Builder.neutralAligned(boolean)`
  and `.dealtInWatheNeutralPool(boolean)` with matching record accessors. The
  8-argument `FactionDefinition` constructor is kept.
- `bindRoleFaction` changes `resolveBaseFaction` for that role (and so its
  capabilities, letter faction text, `/listRoles` group and replay faction)
  to the bound faction; for a native faction also its Wathe faction.
  `getRolesForFaction` now lists bound roles after registered ones.
- `resolveBaseFaction` no longer uses a `switch` and cannot throw for unknown
  constants; one SparkFactionAPI did not append resolves to `wathe:neutral`.
- NoellesRoles needs nothing: on the client, SparkFactionAPI makes its two
  Scavenger hidden-body faction gates (see and target) treat neutral-aligned
  native factions as NEUTRAL. It is pinned to noellesroles-1.7.6-h1.5.6-spark;
  a NoellesRoles build whose lambda or merged handler changed fails client
  startup until the SparkFactionAPI adapters are re-pinned. The client mixin
  config now has a plugin (`SparkFactionClientMixinPlugin`).

## 2026-11-03 Lobby Map Reset (unreleased)

No API signatures change. No migration is required unless a mod relied on
Wathe's start-of-round train copy overwriting the play area.

- When a game start that Wathe would accept (the auto-start countdown reaching
  zero, `/wathe:start` or the horn) finds the map not yet reset,
  SparkFactionAPI defers it at `GameFunctions.startGame` HEAD and first copies
  that world's reset template onto the play area gradually while players wait
  in the lobby; clients show the progress at the top of the lobby HUD. When the
  copy ends, the same start (mode, effect, time) is replayed through Wathe's
  `startGame`, which counts the ready area at that moment, so players can
  still board during the copy. Until then, other starts, `/wathe:mapvote` and
  `/wathe:mapVariables set ...` are refused, and the auto-start countdown waits
  at zero. Starts Wathe would refuse itself (a vote, too few ready players)
  are left to Wathe. Other mixins at `startGame` HEAD may see both the
  deferred call and the replay; the rest of `startGame` runs once.
- For a world reset this way, Wathe's `GameFunctions.tryResetTrain` at round
  start now copies one block instead of the whole template. Blocks and
  block-entity contents (containers included) that add-ons place or change in
  the play area during the first ~0.5 s of a round, before that copy runs, are
  no longer overwritten. The copy still discards bodies, dropped items,
  firecrackers and notes.
- `tryResetTrain`'s signature and return value are unchanged (`false` on
  success), and mixins on it keep working: SparkStrength's
  `TrainResetSabotageMixin` (RETURN) still runs once per successful copy and
  scans the play area as before. Only `getResetTemplateArea()` reads inside
  `tryResetTrain` see the narrowed box; reads anywhere else are unaffected.
- Without a finished lobby reset (a failed or aborted reset,
  `/wathe:mapVariables set ...` since the reset, a changed template area or
  paste offset, or a narrowed copy that asked for a retry), Wathe's full copy
  runs at round start as before.
- A finished lobby reset also discards bodies, dropped items, firecrackers and
  notes in that world, as Wathe's copy does, so this now happens once in the
  lobby too. The train chunks stay loaded, not ticking, from the reset until
  10 s after the round starts (at most 30 minutes).

## 2026-11-02 Team Badge In Rounds (0.1.5.19)

No API signatures change. No migration is required.

- `PlayerEntity#getDisplayName` now carries `` during running Wathe
  rounds too, so server messages, the crosshair name and the end screen show it.
- When a member is also a Ratatouille supporter the order is always name,
  ``, ``, whichever mixin applies first.
- The crosshair name drawn by Wathe's `RoleNameRenderer` is re-matched after all
  disguise wrappers: its text (without E780/E784) is looked up by account name
  among online players, then Wathe's `PLAYER_ENTRIES_CACHE`, and the badge
  follows that player. A disguise that returns another player's plain name gets
  that player's badge; a name that matches nobody (team prefix, "Unknown
  Player") and any obfuscated text (psycho, hallucination) is left as it is.
- NoellesRoles `PuppeteerClient.getName(UUID)` gets the same name-based badge
  (`@Pseudo`, skipped on builds without the Puppeteer).

## 2026-11-02 Caecorthus Team Badge (0.1.5.18)

No API signatures change. No migration is required.

- Team members (listed by the Caecorthus website) get glyph `\uE784` after
  their name: `PlayerEntity#getDisplayName` outside running Wathe rounds (in
  rounds too since 0.1.5.19), and Tab list names always. Code that compares
  `getDisplayName().getString()` with a plain profile name should compare UUIDs
  or `getName()` instead.
- `\uE784` is now taken in `minecraft:default`; pick another private-use
  character for new glyphs.
- Client and server make one HTTPS request to `express-dlc.tlspark.cn` at start
  and every 30 minutes on a daemon thread.

## 2026-10-22 Custom Faction Wins Freeze at Round End (0.1.5.18)

No API signatures change. No migration is required unless a win condition
relied on running after the round was decided.

- `FactionWinCondition.checkWin` now runs only while Wathe's game is ACTIVE.
  Wathe keeps firing `CheckWinCondition` for the roughly 60 STOPPING ticks but
  ignores every result then, so the only effects of running there were side
  effects. SparkWitch's Witch condition, including its Shadow Jester showdown
  trigger, no longer runs during STOPPING.
- `SparkFactionRoundEndComponent` keeps the state set on the winning tick until
  the next round. Before, a winner leaving or dying during STOPPING cleared the
  custom win (end screen, replay title and SparkAssist fell back to the role
  text or default faction), and a Witch condition that started to hold during
  STOPPING could set a custom win over a TIME or other neutral result.

## 2026-10-22 Performance Pass (0.1.5.17)

`api/` is unchanged and the replay wire format is unchanged. No migration is
required for the Spark mods; the notes below matter only for code that relied
on call counts or on internal `impl` helpers.

- Collision exemptions (`EntityCollisionExemption`) are now evaluated only for
  entity pairs that would otherwise collide, and the second entity is skipped
  once the first is exempt. Predicates must stay side-effect free; one that
  counted calls would now see far fewer.
- The NoellesRoles poison-gas guard evaluates `PlayerAffectPolicy` (action
  `noellesroles:poison_gas`) only for players actually inside the gas, instead
  of for every player on every tick. Results are unchanged.
- `SparkFactionRoundEndComponent` syncs only when the winning faction or the
  winner set changes; it used to sync to every player on every round tick.
  Client-visible state is identical, and join-time sync is unchanged.
- Removed `impl` helpers with no callers in any Spark repository:
  `FactionRegistryImpl.isSparkFactionRole` and its five policy-list getters
  (`targetEligibility`, `economyPolicies`, `gunPunishmentPolicies`,
  `blackoutCooldownPolicies`, `instinctPolicies`), the Role-only and unused
  PlayerEntity overloads in `FactionEconomyRules`, `FactionGunPunishmentRules`,
  `FactionTargetRules`, `FactionCohortRules` and `FactionInstinctRules`,
  `FactionCapabilityLookup.hasCustomBaseFaction` and
  `FactionRoundEndTextRules.winPhraseKey`.
  `FactionEconomyRules.receivesCustomKillReward` (used by SparkTraits) stays.
- Additive: `ReplaySnapshot.writeLine` and public codec caps
  (`MAX_PLAYERS`, `MAX_LINES`, `MAX_ROLES`, `MAX_BADGES`, `MAX_LINE_PLAYERS`,
  `MAX_NAME_LENGTH`) with unchanged values.

## 2026-10-21 Settlement Exclusions (0.1.5.16)

Additive and source/binary compatible. No migration is required.

- New `api/settlement/SettlementExclusions`: role ids whose holders never count
  as a living player for round settlement (killer win checks, Last Stand and
  Final Moment thresholds, neutral "last one alive" wins). Role owners call
  `registerRole(Identifier)` during initialization; settlement code skips a
  player when `isExcluded(game, uuid)` / `isExcluded(player)` is true, on top
  of its own alive check. Lookups read the raw role (`getRole`), never
  `isRole`. Nothing is excluded by default, and SparkFactionAPI's own round-end
  paths are unchanged.
- Mods that must load with an older SparkFactionAPI should reach the class by
  reflection and treat its absence as "not excluded".
- Anti-fast-round safe time now also refuses SparkWitch
  `black_raven_crow_toggle`, `black_raven_crow_use` and
  `black_raven_crow_drop_feather`; `black_raven_crow_move` stays allowed.

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
