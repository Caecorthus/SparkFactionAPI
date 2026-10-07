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
