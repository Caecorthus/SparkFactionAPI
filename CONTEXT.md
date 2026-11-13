# SparkFactionAPI Context

This glossary describes the runtime contract vocabulary used by code and tests.

- **Base faction**: the faction declared by a Wathe `Role` mapping. It does not
  include player state, temporary traits, or registered effective-faction
  resolvers.
- **Effective faction**: the player-aware faction after the base faction passes
  through all registered resolvers in registration order.
- **Custom faction**: a faction registered through `SparkFactionApi` rather than
  one of the native Wathe faction buckets.
- **Native Wathe faction**: a custom faction that is also a real constant of
  Wathe's `dev.doctor4t.wathe.api.Faction` enum. SparkFactionAPI appends the
  constant at the end of `Faction`'s static initializer from a
  `sparkfactionapi:native_wathe_factions` declaration in some mod's
  `fabric.mod.json`. The built-ins (`wathe:none/civilian/killer/neutral`) are
  not native custom factions.
- **Bound role**: a role that SparkFactionAPI did not register (for example a
  NoellesRoles role) attached by role id to a registered custom faction with
  `SparkFactionApi.bindRoleFaction`.
- **Neutral-aligned**: `SparkFactionApi.isNeutralAligned(Role)`; the role's
  Wathe faction is NEUTRAL, or is the constant of a native faction whose
  definition has `neutralAligned(true)`. It is the test for the "neutral perks"
  that native factions keep after `Role#isNeutral` became false for them.
- **Policy `null`**: abstention, not denial. Evaluation continues or falls back
  through the registered policy chain before capability or native fallback.
- **Registration order**: the order in which downstream integrations register
  resolvers or policies. It is observable public behavior.
- **Adapter**: a mixin, component hook, or networking hook that locates an
  external lifecycle seam and delegates decisions to an owning internal rule.
- **Downstream consumer**: a mod that compiles against classes in `api/`.
  Identifier references or optional metadata alone do not make a Java consumer.
- **Replay role timeline**: the per-player role history of one match, built from
  Wathe's opening `role_assigned` snapshot plus `sparkfactionapi:role_changed`
  events. A replay line shows the role held immediately before its event.
- **Role-change cause**: an identifier attached through
  `SparkReplayApi.withRoleChangeCause`; it selects the optional replay suffix key
  `replay.sparkfactionapi.role_changed.cause.<ns>.<path>[.by]`.
- **Match record payload**: the structured, non-display copy of one finished
  Wathe `MatchRecord` (`MatchRecordSnapshot`: event `seq`, `type`, `tick` since
  the match start, and Wathe's NBT `data`), pushed to clients at round end and
  read on the client through `client.api.SparkMatchRecordClient.latest()`.
- **Kill-start snapshot**: the victim's and killer's role, effective faction,
  psycho state, the killer's main-hand item and the feet-to-feet distance, read
  when `GameFunctions.killPlayer` starts and written onto that kill's Wathe
  `death` record. "Kill start" is before every HEAD injector and before Wathe
  clears psycho mode or any mod changes roles.
- **Achievement record events**: the namespaced match-record events
  `sparkfactionapi:psycho`, `sparkfactionapi:consume` and
  `sparkfactionapi:income` plus the extra `death` fields, written for
  SparkAssist's local achievements. They have no replay formatter, so the
  replay never shows them.
- **Match income**: the coins a player gained in one match through Wathe
  `PlayerShopComponent#addToBalance` on the server, i.e. the sum of the positive
  actual balance changes (after − before) of those calls. Starting money and
  other `setBalance` writes are not income.
- **Hidden equipment registration**: an item-identity registration through
  `api/compat/NoellesHiddenEquipment`. When NoellesRoles is present, the
  optional Adapter adds registered items to its existing held-item hiding path.
- **Limited inventory second row**: player-inventory main slots 27-35, shown
  directly above the hotbar in Wathe's `LimitedInventoryScreen`. Main slots
  9-26 stay hidden storage for add-ons. "In the limited inventory" means alive,
  survival, and Wathe `TrainWorldComponent#hasHud`, the same condition under
  which Wathe swaps in that screen.
- **Settlement exclusion**: a role registered through
  `api/settlement/SettlementExclusions` whose holders never count as a living
  player for round settlement in any mod (killer win checks, SparkTraits Last
  Stand and Final Moment thresholds, neutral "last one alive" wins). It is a
  classification read by each settlement point; it changes no win rule, event or
  alive state by itself.
- **Forced cooldown**: a cooldown imposed on a player by another feature (penalty,
  aura, debuff) through `api/cooldown/ForcedCooldowns`, covering both
  registered role-skill stores (`RoleSkillCooldownStore`) and vanilla item
  cooldowns on carried items. A `CooldownSlot` is a read-only snapshot; writes
  re-read the live value.
- **Anti-fast-round safe time**: an opt-in window of N seconds, set with
  `/sparkfactionapi:antifastround`, that opens at Wathe
  `GameEvents.ON_FINISH_INITIALIZE` and closes on time, at `ON_FINISH_FINALIZE`,
  or when an administrator disables the feature. It applies to players alive in
  a running round who are not creative or spectators: item use (right-click use,
  item-on-block, item-on-entity), payload-fired item uses, and the role-skill
  C2S payloads listed in `AntiFastRoundRules.BLOCKED_PAYLOADS` are refused, and
  their entity attacks are cancelled before default-phase listeners run, so hits
  deal no knockback. Bare-hand block use, bare-hand use of non-player entities, and the
  shop stay allowed. The N seconds count from the end of Wathe's ~3 s fade-in
  (the lock already holds during it). Notices use the action bar because Wathe
  hides chat in-game. Clients stay locked until the server's close sync arrives.
- **Lobby map reset**: the gradual copy of a map's Wathe reset template
  (`resetTemplateArea` moved by `resetPasteOffset`) onto its play area while
  players wait in the lobby. It starts when a game start that Wathe would
  accept (the auto-start countdown reaching zero, `/wathe:start` or the train
  horn) finds the world not prepared: the start is deferred, and once the reset
  ends it is replayed with the same mode, effect and time. It runs the passes,
  order and block flags of Wathe's `GameFunctions.tryResetTrain`, but spends at
  most 8 ms of server time per tick. One reset runs at a time; other starts are
  refused while it runs.
- **Prepared map**: a world whose lobby map reset finished and whose reset
  template area and paste offset still equal the ones that reset copied. A
  start on a prepared world goes straight to Wathe, whose start-of-round copy
  is narrowed to one probe block; that copy consumes the state. Anything else
  is not prepared and gets Wathe's original full copy: a failed or aborted
  reset, a template area or paste offset changed since the reset, a narrowed
  copy that asked for a retry, or any `/wathe:mapVariables set ...` run outside
  a reset.

## Stable Runtime Contracts

- Component id: `sparkfactionapi:round_end`.
- Persisted round-end keys: `WinningFaction` and `Winners`.
- Custom wins return Wathe `NEUTRAL`; winner UUID rows are supplied by the
  FactionAPI round-end state path. This holds for native Wathe factions too:
  `WinStatus` is never extended.
- Native Wathe faction declaration (since 0.1.6.0): `fabric.mod.json`
  `custom."sparkfactionapi:native_wathe_factions"` is a JSON object of
  `"namespace:path": "CONSTANT_NAME"`. Ids need an explicit namespace other than
  `wathe`; names match `[A-Z][A-Z0-9_]*` and are not existing constants; one id
  has one name and one name one id (the same pair from several mods merges).
  Anything else fails `Faction` class initialization (a startup crash, logged at
  ERROR). The constants are appended after the built-ins, sorted by the id string,
  with consecutive ordinals, so client and server agree whatever the mod order.
  The append runs at TAIL of `Faction.<clinit>` (`FactionEnumExtensionMixin`
  through `FactionEnumAccessor`'s `$VALUES` setter and `<init>` invoker) and
  never calls `Faction.fromRole`, a SparkFactionAPI registry, `valueOf`,
  `EnumMap` or `EnumSet`, so `values()`, `valueOf`, `EnumMap` and switch maps
  see the extended set. One INFO line lists the appended constants.
- `factionIdOf(Faction)` maps the built-ins to `wathe:*` and an appended
  constant to its declared id (empty for null or a constant SparkFactionAPI did
  not append). Wathe's `role_assigned` record uses `Faction.name()`, so native
  roles show `WITCH`-style names there.
- Wathe faction of a role: an SFA role registered with a NONE bucket reads as
  its faction's constant when that faction is native (`isNeutral` false,
  `isInnocent` and `canUseKiller` false), otherwise as NEUTRAL (`isNeutral`
  true), as before. An explicit CIVILIAN, KILLER or NEUTRAL bucket keeps Wathe's
  own answer; an explicit appended constant must be the faction's own. A bound
  role reads as its faction's constant when that faction is native
  (`isNeutral` false) and is otherwise untouched by Wathe. Every other role is
  unchanged.
- `bindRoleFaction(roleId, factionId)`: the faction must be a registered custom
  faction and the role must not be registered through SparkFactionAPI
  (`IllegalArgumentException`); rebinding to another faction throws
  `IllegalStateException`, the same pair again is a no-op; a bound id cannot be
  registered later. Resolved lazily by `Role#identifier()` on both sides, so
  Wathe registration order does not matter. Bound roles are listed after
  registered ones by the internal `FactionRoleCatalog.getRolesForFaction` once
  Wathe knows them.
- `resolveBaseFaction` never throws: registered faction, then bound faction,
  then a switch-free reverse lookup of `role.getFaction()`; a constant that
  SparkFactionAPI did not append resolves to `wathe:neutral`. No SparkFactionAPI
  class switches over `Faction`.
- `FactionDefinition` flags (since 0.1.6.0, default false):
  `dealtInWatheNeutralPool` makes the two `role.getFaction()` reads in Wathe's
  `ScoreboardRoleSelectorComponent.assignNeutrals` (already-assigned neutral
  count, candidate filter) return NEUTRAL for that native faction's roles, so
  they are drawn with Wathe's neutral odds and take a neutral slot; nothing else
  sees it (`@WrapOperation`, require = allow = 2). Registering it on a faction
  that is not declared native throws `IllegalArgumentException`.
  `neutralAligned` feeds `isNeutralAligned`; on a non-native faction it is
  logged as a warning and has no effect. Roles of a native faction without
  `dealtInWatheNeutralPool` are drawn by no Wathe pass and do not consume a
  neutral slot.
- NoellesRoles Scavenger hidden bodies (D3, client only, pinned to
  noellesroles-1.7.6-h1.5.6-spark, sha256 fcb0da69...): NoellesRoles' two
  `KILLER || NEUTRAL` faction gates, the only ones in that jar, read
  `NoellesNeutralPerkRules.neutralPerkFaction`, which returns NEUTRAL for an
  appended neutral-aligned native constant and any other faction unchanged, so
  witch and Jiahao roles still see and target hidden bodies. Targeting: a
  `@Pseudo` `@WrapOperation` (require = allow = 1) on the `Role#getFaction`
  read in `NoellesrolesClient.lambda$onInitializeClient$6` (the
  `CanTargetBody` listener, selected by name). Visibility: NoellesRoles'
  `ScavengerBodyHideMixin.hideScavengerBodies` is merged into Wathe's
  `PlayerBodyEntityRenderer` under a per-session unique name that no selector or
  injection point can reach, so the client config's
  `SparkFactionClientMixinPlugin` rewrites its two `getFaction()` calls to
  `NoellesNeutralPerkRules.neutralPerkFactionOf(Role)` in `postApply` of the
  empty anchor `NoellesScavengerBodyHideNeutralPerkMixin` (skipped without
  NoellesRoles), after every mixin has been applied. The handler is found by
  `@MixinMerged(mixin = ...)` and its original name. Any other shape (no or
  several handlers, a faction read that is not a KILLER/NEUTRAL comparison)
  throws at startup, like a stale `require = 1` selector. Hiding, `canHit`
  and every other NoellesRoles path are unchanged. The local
  `NoellesRolesPinnedSelectorTest` resolves both selectors against that jar.
- Version protocol channel, payload order, rejection behavior, policy ordering,
  and capability fallbacks are compatibility-sensitive.
- Hidden-equipment registration is idempotent and monotonic: registrations may
  add hidden items but never unhide an item selected by NoellesRoles. With
  NoellesRoles absent, registration remains inert and does not fail loading.
- Settlement exclusions are identifier-only, idempotent and monotonic, with no
  defaults: `registerRole` (null throws, returns whether the id was new),
  `containsRole`, `isExcludedRole`. The player lookup
  `isExcluded(GameWorldComponent, UUID)` reads the raw role through
  `GameWorldComponent#getRole(UUID)`, never `isRole`, so acting or disguise
  overlays cannot change it; it does not check liveness and returns false for
  null inputs or no role. Owners
  register during mod initialization; reads run on the server thread during a
  round. Callers built against an older SparkFactionAPI must treat a missing
  class as "not excluded" (the previous behavior).
- Forced cooldowns are monotonic and exact. `raise` writes only when the new
  remaining value exceeds the current one; `extend` adds to the remaining value
  (saturating, ready slots restart) and ignores non-positive amounts. Item
  writes bypass `ItemCooldownManager.set` duration modifiers by rewriting the
  vanilla entry in place and sending an exact `CooldownUpdateS2CPacket`.
  `slots` order is role-skill stores in registration order (only when
  `appliesTo` and `mayForce` hold), then distinct non-exempt carried items in
  main inventory, offhand, armor order, including ready items. Item nominal
  lookup is providers in registration order (first present wins), then Wathe
  `GameConstants.ITEM_COOLDOWNS`. Items whose registry
  id is `noellesroles:timed_bomb` are exempt by default; registered exemptions
  only add to that. Writes re-check `appliesTo`/`mayForce`, exemption, and
  possession; a throwing store, provider, or exemption is logged and isolated
  (exemptions fail closed). Player-taking entry points throw
  `IllegalStateException` off the server thread.
- `ForcedCooldowns.clearItemKeepingForced(player, item)` is the role-mechanic
  clear (refreshes, resets on kill). It runs a plain
  `ItemCooldownManager.remove`, so owner `remove` hooks still release their own
  timers. It then re-installs, exactly, the forced part of that item's cooldown
  that is not yet served, capped at what the item had left, and returns those
  ticks (0 = ready). Each item `raise`/`extend` that passes the exemption and
  possession checks records its forced part on the live vanilla entry, keyed
  weakly by entry identity, so a later plain `remove` (admin `clearCooldown`,
  Wathe reset) or `set` drops it. A raise is a floor that runs from its write
  (recorded even when a longer natural cooldown covers it); an extend is
  appended after the time already there and is served only after it.
  Natural, and cleared: the item's own use cooldown and owner timers mirrored
  onto it (SparkWitch Clock, Gift Watch, Ceremonial Sword, White Cane, fish).
  Role-skill stores are never read or written by it.

- Replay event type: `sparkfactionapi:role_changed` with NBT keys `player`
  (UUID), `from`, `to` (role ids), optional `cause` (id) and `source` (UUID).
  Recorded only server-side, during an active Wathe match, outside
  `readFromNbt`, when the role id actually changes.
- Replay tooltip contributors run in registration order while Wathe generates
  the replay, before players are reset; a failing contributor is skipped.
- Replay screen payload: S2C `sparkfactionapi:replay_snapshot` carrying a
  `ReplaySnapshot` (header, roster with role steps, tooltip and badges, and
  categorized lines). Player names inside lines carry a `ReplayTextTags`
  insertion instead of a hover; the client resolves tooltips by UUID. Sent only
  on request.
- After a match, clients that registered the payload get a short chat summary
  with "open replay" and "show in chat" buttons; other clients keep the full
  chat replay.
- `/replay` opens the replay screen when the client registered
  `sparkfactionapi:replay_snapshot` and a snapshot exists, otherwise resends the
  chat replay; `/replay chat` always resends chat.
- Match record payload: S2C `sparkfactionapi:match_record`, sent from Wathe
  `RecordEvents.ON_RECORD_END` (inside `GameFunctions.finalizeGame`, before
  `AnnounceEndingPayload` and the INACTIVE status sync) to every online player
  whose client registered it. Events keep Wathe record order, skip
  `door_interaction`, and keep only the earliest 8192 that also fit a 900 KiB
  budget; `tick` is clamped to 0 or more. A failure is logged and never stops
  finalization. The client stores only the latest snapshot, clears it on
  disconnect, and opens nothing. `SparkMatchRecordClient.latest()` and the
  `MatchRecordSnapshot` accessors are read reflectively by SparkAssist.
- Extra `death` record fields (achievement record contract A1), added by
  wrapping the 5-argument `GameFunctions.killPlayer` (the 3- and 4-argument
  overloads delegate to it) and modifying the data argument of
  `GameRecordManager.recordDeath`'s `addEvent` call: `victim_role` (string,
  omitted without a role or for `wathe:no_role`), `victim_faction` (string),
  `victim_psycho` (bool); with a killer, `killer_role` (string, same omission),
  `killer_faction` (string), `killer_psycho` (bool), `killer_item` (string,
  `minecraft:air` when empty) and, in the same world only, `distance` (double,
  blocks). Effective faction is `SparkFactionApi.resolveEffectiveFaction`;
  psycho is `getPsychoTicks() > 0`. Snapshots live in a per-thread stack keyed
  by victim and popped in `finally`, so a kill cancelled at HEAD, in
  `KillPlayer.BEFORE` or by the psycho shield records nothing and leaves
  nothing behind, and a kill nested in another kill uses its own snapshot.
  Killer fields are written only when `recordDeath`'s killer is the captured
  killer.
- `sparkfactionapi:psycho` (actor, `active` bool) is recorded from Wathe
  `PsychoModeEvents.ON_PSYCHO_START/END` (server only, every psycho source).
  Per match, an END counts only after a counted START and a repeated START is a
  refresh, because Wathe's `stopPsycho` fires END unconditionally (game-start
  and lobby `reset()`, Noelle's Jester reset) and `startPsycho` refires START.
  Psycho ticks restored from player NBT (rejoin) fire no START, so that
  psycho's later END is ignored.
- `sparkfactionapi:consume` (actor = consumer, `item` id, `kind` `food` or
  `drink`) is recorded after `ItemStack#finishUsing` returns for a server
  player (every finished eat/drink use, including item overrides that skip
  `super`) when the stack was used or replaced, so a refusal that returns the
  untouched stack (SparkStrength's Coroner and base spirits) and a creative
  player whose stack is not used are not recorded. It is also recorded at
  Wathe `PoisonUtils.applyFoodPoison(target, stack)` when the target is not
  inside its own `finishUsing` (Noelle's Waiter feeding, SparkStrength
  capsules). A drink is a Wathe `CocktailItem` (and subclasses) or an item
  whose use action is DRINK; otherwise an item with a food component is food;
  anything else is not recorded. Not covered: a capsule-delivered Blue
  Belladonna (calls neither seam) and instant items without a food component
  or DRINK action (SparkWitch Fisher fish, SparkStrength Professor serum).
- `sparkfactionapi:income` (`actor` = player, `amount` int; contract A4) is
  written once per player with positive match income at the head of
  `GameRecordManager.endMatch`, while the match is still active and before
  `match_end`. `actor` is the online player, otherwise the UUID through
  `putUuid("actor", …)`. A `@WrapMethod` on `addToBalance` reads the personal
  balance around the call on the server during an active match; it composes
  with SparkStrength's killer-team purse wrapper (either order sees the same
  change) and includes SparkTraits' Snowball payout on the inner `setBalance`.
  A call nested in an open call for the same player is measured by the outer
  call only. Not income: starting money and every other `setBalance` write
  (the set-money command, transfers), purchases and deductions, Wathe's
  dev-environment auto-balance (a direct field write), and money credited
  somewhere other than the personal balance (SparkWitch's disguised Black
  Raven wallet, the SparkStrength killer-team purse). Totals are kept per match
  id and forgotten once `startMatch` has created the next match, so a round
  that never reached `endMatch` cannot leak into the next one.
- Every achievement-record hook logs and swallows its own failure; killing,
  eating, psycho mode and balance changes behave exactly as before.
- In the limited inventory, `PlayerInventory#getEmptySlot` returns hotbar 0-8,
  then second row 27-35, then hidden 9-26; capacity and stack merging stay
  vanilla. A default Wathe shop purchase (no custom buy handler) whose hotbar is
  full is placed with `setStack` in the first empty second-row slot. The static
  `ShopEntry.insertStackInFreeSlot` stays hotbar-only. While psycho ticks are
  positive, slot clicks on second-row slots are ignored.
- Anti-fast-round component id: `sparkfactionapi:anti_fast_round` (scoreboard).
  Persisted keys `Enabled` (default false) and `Seconds` (default 10, clamped to
  1-600). The safe-time window is runtime-only: synced to clients so they can
  predict the lock, never saved. A new skill payload is blocked only once it is
  classified as a row of `config/payload-policy/actions.tsv`, the one table of
  locked-player payload decisions for SparkFactionAPI, SparkWitch and
  SparkStrength; `tools/payload_policy.py generate` turns its SAFE_TIME column
  into `GeneratedPayloadPolicy`, which `AntiFastRoundRules.BLOCKED_PAYLOADS`
  aliases, and `check` fails on a stale generated file or an unlisted C2S id.
- Lobby map reset component id: `sparkfactionapi:map_reset` (scoreboard,
  optional on clients, runtime-only: nothing is saved). Its synced progress is
  `-1` (nothing to show), `0`-`99` (a reset runs; capped at 99 until the copy
  is done) or `100` (complete, held about 3 s, then `-1`). A failed or aborted
  reset goes straight to `-1`. Clients draw it as a lobby HUD line at the top
  of the screen, never during a round.
- The lobby map reset is triggered at `GameFunctions.startGame` HEAD, before
  Wathe stores the mode, effect or time. Wathe calls it only from the
  auto-start countdown reaching zero, `/wathe:start` and the train horn. A
  start Wathe would accept (`INACTIVE`, no vote active, ready players at least
  the game mode's `minPlayerCount`) on a world that is not prepared and has a
  reset template area and paste offset is cancelled, begins the reset, and
  keeps its mode, effect and time. When the reset ends, successful or not, the
  start is replayed through Wathe's own `startGame` (not intercepted again),
  so the ready area is counted at that moment, players who boarded during the
  reset count, and Wathe's usual notices appear; after a failed reset Wathe
  does its full copy. A replay Wathe refuses leaves the world prepared, and the
  next accepted start goes straight to Wathe. Starts Wathe would refuse itself,
  and starts on a prepared world, reach Wathe unchanged.
- While a lobby map reset runs: any other `GameFunctions.startGame` call is
  refused at HEAD with the action-bar notice
  `message.sparkfactionapi.map_reset.start_blocked`; Wathe's auto-start
  countdown, which would otherwise restart from the full start time once it
  fired, waits at zero on both sides until the replayed start takes over, and
  still restarts when the ready area drops below the minimum; commands executed
  through `CommandManager.execute` (players, console, command blocks) whose
  literal chain contains `wathe:mapvote`, or `wathe:mapVariables` directly
  followed by `set`, are refused, also behind `execute ... run`. Matching is
  exact and case-sensitive; `/wathe:mapVariables help` stays available.
  Datapack functions skip `CommandManager.execute`, so
  `MapVotingComponent.startVoting` is also refused at HEAD (logged); a
  map-variables write from a function is caught afterwards by the prepared-map
  template check. The auto-start hold never applies while a vote is active,
  and clients hold on the synced progress alone, without their own
  ready-player count. A round that starts anyway fails the reset and drops the
  deferred start.
- For a prepared map, a `@WrapOperation` on every
  `MapVariablesWorldComponent#getResetTemplateArea()` read inside
  `GameFunctions.tryResetTrain` returns a zero-size box at the probe block:
  an interior air block whose six neighbors are air when the template has one,
  otherwise any air block, otherwise any non-barrier block without a block
  entity (a template with none is never prepared). Wathe's copy still checks
  loading, changes that one block (barrier, then its template state), discards
  bodies, items, firecrackers and notes, and returns `false` ("no retry"). The
  copy is narrowed instead of skipped so that injectors on `tryResetTrain`
  RETURN keep firing on a real success: SparkStrength's
  `TrainResetSabotageMixin` scans the play area for junction boxes and breakers
  only then. The signature is unchanged. Any return consumes the prepared
  state: after `false` a later Wathe copy in that world is a full copy unless
  a new lobby reset prepared it again, and a `true` ("retry") makes the retry
  Wathe's full copy, so a narrowed copy can never retry forever.
- The template and paste chunks, plus one chunk of margin, are held with
  FULL-level tickets (`sparkfactionapi_map_reset`, radius 0: loaded, not
  ticking) during the copy, and after it until 10 s after that world's next
  successful Wathe copy, 30 minutes without one, or the next reset. A failed
  or aborted reset releases them at once.
- A creative player can use dye, a glow ink sac or an ink sac on a waxed sign
  (standing, wall or hanging) as on an unwaxed one; the sign stays waxed and its
  text stays locked. Honeycomb and every non-creative player keep vanilla
  behavior, so players in a round (adventure mode) cannot recolor signs. A sign
  in the map's Wathe `interaction_blacklist` stays blocked for everyone.
- Caecorthus team badge: glyph `\uE784` (font `minecraft:default`, texture
  `sparkfactionapi:font/caecorthus_team.png`; E780 is Ratatouille, E781 Wathe,
  E782/E783 SparkWitch). Members are the UUIDs from the website's
  `GET https://express-dlc.tlspark.cn/api/team/minecraft`
  (`{"members": [{"uuid", "groups"}]}`), fetched by client and server at start
  and every 30 minutes; a failed or malformed fetch keeps the last list, cached
  in `config/sparkfactionapi/caecorthus_team.json`. The badge is appended once,
  white, to `PlayerEntity#getDisplayName` and Tab list names, in and out of
  rounds, before a trailing Ratatouille E780 (name, E784, E780). Wathe's
  crosshair name is re-matched after every disguise wrapper: the drawn name is
  looked up by account name and carries that player's badge, so a disguise shows
  the imitated player's badge and never the disguiser's. NoellesRoles builds with
  the Puppeteer get the same name-based badge on `PuppeteerClient.getName`.
  Obfuscated text and names matching no player are left as they are. Wathe's
  letter name strips both E780 and E784.

## Current Dependency Picture

- Wathe is the hard host dependency and exact mixin target.
- NoellesRoles is an optional compatibility target, never a hard dependency.
- SparkWitch is the direct public-API consumer, including replay role-change
  causes. It declares the native Wathe factions (`sparkwitch:witch`,
  `sparkwitch:jiahao`) and binds `noellesroles:corrupt_cop` by id.
- Wathe `Faction` is extended in place; Wathe itself is not edited. Its only
  switch over `Faction` (`GameSettingsCommand.listRoles`) is cancelled at HEAD
  by `GameSettingsCommandMixin`.
- SparkTraits may integrate through optional public seams; it contributes
  replay trait tooltips and trait badges through `api/replay`.
- `client/api/card` (`InventoryCardLayout`, `InventoryCardPaint`,
  `InventoryInfoCard`, `GuideDecorBridge`) is the single shared owner
  inventory info card engine: SparkTraits uses it now and SparkWitch is moving
  to it. Each mod creates its own `InventoryInfoCard` (own caches, overflow
  lang key, second-row rule, HUD and decoration hooks) and its own
  `GuideDecorBridge`; instances are never shared between mods.
  SparkAssist decoration is reached by name only.
- `api/settlement/SettlementExclusions`: SparkWitch registers
  `sparkwitch:black_raven` by reflection (it compiles against an older
  SparkFactionAPI and fails closed); SparkTraits and SparkStrength read it,
  optionally, at their own settlement points.
- SparkStrength and SparkAssist are not current Java API consumers.
  SparkStrength reaches `api/cooldown` (store registration,
  `clearItemKeepingForced`) only by reflection and falls back when it is
  missing. SparkAssist reads the match record through
  `client.api.SparkMatchRecordClient` only by reflection, including the
  achievement record events and `death` fields by name.
