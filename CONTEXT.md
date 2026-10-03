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
- **Hidden equipment registration**: an item-identity registration through
  `api/compat/NoellesHiddenEquipment`. When NoellesRoles is present, the
  optional Adapter adds registered items to its existing held-item hiding path.
- **Forced cooldown**: a cooldown imposed on a player by another feature (penalty,
  aura, debuff) through `api/cooldown/ForcedCooldowns`, covering both
  registered role-skill stores (`RoleSkillCooldownStore`) and vanilla item
  cooldowns on carried items. A `CooldownSlot` is a read-only snapshot; writes
  re-read the live value.

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

## Current Dependency Picture

- Wathe is the hard host dependency and exact mixin target.
- NoellesRoles is an optional compatibility target, never a hard dependency.
- SparkWitch is the direct public-API consumer.
- SparkTraits may integrate through optional public seams.
- SparkStrength and SparkAssist are not current Java API consumers.
