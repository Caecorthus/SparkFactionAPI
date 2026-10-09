# SparkFactionAPI Context

This glossary describes the runtime contract vocabulary used by code and tests.

- **Base faction**: the faction declared by a Wathe `Role` mapping. It does not
  include player state, temporary traits, or registered effective-faction
  resolvers.
- **Effective faction**: the player-aware faction after the base faction passes
  through all registered resolvers in registration order.
- **Custom faction**: a faction registered through `SparkFactionApi` rather than
  one of the native Wathe faction buckets.
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

## Stable Runtime Contracts

- Component id: `sparkfactionapi:round_end`.
- Persisted round-end keys: `WinningFaction` and `Winners`.
- Custom wins return Wathe `NEUTRAL`; winner UUID rows are supplied by the
  FactionAPI round-end state path.
- Version protocol channel, payload order, rejection behavior, policy ordering,
  and capability fallbacks are compatibility-sensitive.
- Hidden-equipment registration is idempotent and monotonic: registrations may
  add hidden items but never unhide an item selected by NoellesRoles. With
  NoellesRoles absent, registration remains inert and does not fail loading.
- Forced cooldowns are monotonic and exact. `raise` writes only when the new
  remaining value exceeds the current one; `extend` adds to the remaining value
  (saturating, ready slots restart) and ignores non-positive amounts. Item
  writes bypass `ItemCooldownManager.set` duration modifiers by rewriting the
  vanilla entry in place and sending an exact `CooldownUpdateS2CPacket`.
  `slots` order is role-skill stores in registration order (only when
  `appliesTo` and `mayForce` hold), then distinct non-exempt carried items in
  main inventory, offhand, armor order, including ready items. Item nominal
  lookup is explicit value (last wins), providers in registration order (first
  present wins), then Wathe `GameConstants.ITEM_COOLDOWNS`. Items whose registry
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
  classified in `AntiFastRoundRules.BLOCKED_PAYLOADS`.

## Current Dependency Picture

- Wathe is the hard host dependency and exact mixin target.
- NoellesRoles is an optional compatibility target, never a hard dependency.
- SparkWitch is the direct public-API consumer, including replay role-change
  causes.
- SparkTraits may integrate through optional public seams; it contributes
  replay trait tooltips and trait badges through `api/replay`.
- SparkStrength and SparkAssist are not current Java API consumers.
  SparkStrength reaches `api/cooldown` (store registration,
  `clearItemKeepingForced`) only by reflection and falls back when it is
  missing. SparkAssist reads the match record through
  `client.api.SparkMatchRecordClient` only by reflection, including the
  achievement record events and `death` fields by name.
