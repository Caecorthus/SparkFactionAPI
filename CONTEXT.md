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

- Replay event type: `sparkfactionapi:role_changed` with NBT keys `player`
  (UUID), `from`, `to` (role ids), optional `cause` (id) and `source` (UUID).
  Recorded only server-side, during an active Wathe match, outside
  `readFromNbt`, when the role id actually changes.
- Replay tooltip contributors run in registration order while Wathe generates
  the replay, before players are reset; a failing contributor is skipped.
- `/replay` resends the latest generated replay to the calling player.

## Current Dependency Picture

- Wathe is the hard host dependency and exact mixin target.
- NoellesRoles is an optional compatibility target, never a hard dependency.
- SparkWitch is the direct public-API consumer, including replay role-change
  causes.
- SparkTraits may integrate through optional public seams; it contributes
  replay trait tooltips through `api/replay`.
- SparkStrength and SparkAssist are not current Java API consumers.
