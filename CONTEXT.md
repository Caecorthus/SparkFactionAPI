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

## Stable Runtime Contracts

- Component id: `sparkfactionapi:round_end`.
- Persisted round-end keys: `WinningFaction` and `Winners`.
- Custom wins return Wathe `NEUTRAL`; winner UUID rows are supplied by the
  FactionAPI round-end state path.
- Version protocol channel, payload order, rejection behavior, policy ordering,
  and capability fallbacks are compatibility-sensitive.

## Current Dependency Picture

- Wathe is the hard host dependency and exact mixin target.
- SparkWitch is the direct public-API consumer.
- SparkTraits may integrate through optional public seams.
- SparkStrength and SparkAssist are not current Java API consumers.
