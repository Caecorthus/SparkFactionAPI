# SparkFactionAPI Architecture Logs

This file records architecture closure and legacy-migration history extracted
from `ARCHITECTURE.md`.

Read `ARCHITECTURE.md` first. These notes constrain old seams, but they do not
authorize edits. Approval, behavior-invariant, downstream-impact, and
verification rules remain in `ARCHITECTURE.md`.

## Architecture Closure Log

### Closed Boards

- Registry and policy ownership is migrated into `impl/registry/` plus owning
  policy Modules. `FactionRegistryImpl` remains a compatibility Facade and must
  not regain registry state.
- Round-end semantics are migrated into `impl/roundend/` plus CCA storage in
  `component/`. Revisit only if a concrete round-end bug or feature needs it.
- Capability lookup is migrated into `impl/capability/FactionCapabilityLookup`.
  It must stay lookup-only and must not recreate `FactionCapabilityBridge`.
- Game-settings command text is migrated into
  `command/settings/GameSettingsCommandRules`; the mixin is a thin Adapter.
- `impl/FactionCompatibilityEvents.java` is watch-only. It is a thin event
  registration aggregator and is not worth splitting unless event ordering grows
  or additional lifecycle hooks make Locality worse.

## Legacy Register

This register records known non-target modules. It does not authorize edits.

### `impl/FactionCapabilityBridge.java`

Status: Retired.

Reason: its remaining responsibilities were split into owning domain Modules.
Keeping the bridge would create a shallow pass-through Module and invite future
behavior to flow back into a deleted seam.

Rule: `impl/FactionCapabilityBridge.java` must not exist. Do not restore it for
convenience. Put behavior in the owning domain Module and route callers there.

Gun punishment rules and gun punishment policy ownership have migrated to
`impl/gun/` and must not be re-added to this bridge.

Economy reward rules and economy policy ownership have migrated to
`impl/economy/` and must not be re-added to this bridge.

Blackout rules and blackout cooldown policy ownership have migrated to
`impl/blackout/` and must not be re-added to this bridge.

Visual cohort rules, instinct rules, and instinct policy ownership have
migrated to `impl/vision/` and must not be re-added to this bridge.

Target eligibility rules have migrated to `impl/target/` and must not be
re-added to this bridge.

Killer shop access rules have migrated to `impl/shop/` and must not be re-added
to this bridge.

### `impl/capability/FactionCapabilityLookup.java`

Status: Migrated.

Current location: `impl/capability/`.

Rule: this Module may only compose existing registry lookup primitives:
`resolveBaseFaction`, `resolveEffectiveFaction`, `capabilities`, and
`isCustomFaction`. It may expose internal helpers for base/effective faction ids,
base/effective capabilities, and custom base/effective faction checks. It must
not own economy, gun, blackout, cohort, instinct, target, shop, assignment, or
round-end policy decisions. Do not add policy lists, priority rules,
first-non-null ordering, highest-priority ordering, mixin logic, or public
downstream Interface here.

### `impl/FactionRegistryImpl.java` and `impl/registry/`

Status: Migrated Facade.

Current location: `impl/FactionRegistryImpl.java` delegates to
`impl/registry/`.

Rule: faction definition state and legacy faction capability defaults must stay
in `impl/registry/FactionCatalog.java`. Role registration, role-to-faction
mapping, and Wathe native-bucket shielding must stay in
`impl/registry/FactionRoleCatalog.java`. Bootstrap ownership must stay in
`impl/registry/FactionRegistryBootstrap.java`. Effective-faction resolver
registration and ordered evaluation must stay in
`impl/registry/EffectiveFactionResolvers.java`. `FactionRegistryImpl` may
remain as a compatibility Facade, but must not regain registry state.

### `impl/FactionCompatibilityEvents.java`

Status: Watch-Only.

Rule: keep only event registration aggregation here. Do not split this Module
unless event ordering grows, lifecycle hooks multiply, or a concrete bug proves
the aggregation is hiding domain behavior.

### `impl/gun/FactionGunPunishmentAdapter.java` and
`impl/gun/FactionGunPunishmentRules.java` and
`impl/gun/FactionGunPunishmentPolicies.java`

Status: Migrated.

Current location: `impl/gun/`.

Rule: innocent-shot punishment policy evaluation, custom victim/shooter
decisions, gun punishment policy registration and ordering, and killer-like
revolver consumption must stay in `impl/gun/`. Public callers should continue
using `SparkFactionApi.registerGunPunishmentPolicy(...)`.
`FactionCapabilityBridge` must not regain gun punishment methods.

### `impl/economy/FactionEconomyRules.java`,
`impl/economy/FactionEconomyPolicies.java`,
`impl/economy/FactionPassiveMoneyAdapter.java`, and
`impl/economy/FactionKillRewardAdapter.java`

Status: Migrated.

Current location: `impl/economy/`.

Rule: passive killer money tick routing, direct kill rewards, custom-only
reward compensation, poisoner fallback routing for direct-kill reward
compensation, economy policy registration and ordering, and
`FactionEconomyPolicy` first-non-null evaluation must stay in `impl/economy/`.
Public callers should continue using `SparkFactionApi.registerEconomyPolicy(...)`.
`FactionCapabilityBridge` must not regain economy reward methods.

### `impl/blackout/FactionBlackoutCooldownAdapter.java` and
`impl/blackout/FactionBlackoutRules.java` and
`impl/blackout/FactionBlackoutCooldownPolicies.java`

Status: Migrated.

Current location: `impl/blackout/`.

Rule: blackout immunity, legacy killer-feature fallback for blackout night
vision, blackout cooldown policy registration and ordering, blackout cooldown
policy evaluation, and custom-only blackout cooldown sharing must stay in
`impl/blackout/`. Public callers should continue using
`SparkFactionApi.registerBlackoutCooldownPolicy(...)`. `FactionCapabilityBridge`
must not regain blackout methods.

### `impl/vision/FactionCohortRules.java` and
`impl/vision/FactionInstinctRules.java` and
`impl/vision/FactionInstinctPolicies.java`

Status: Migrated.

Current location: `impl/vision/`.

Rule: visual cohort sharing, instinct eligibility, instinct display colors,
spectator fallthrough, instinct policy registration and ordering, and
`FactionInstinctPolicy` highest-priority non-null evaluation must stay in
`impl/vision/`. Public callers should continue using
`SparkFactionApi.registerInstinctPolicy(...)`. `FactionCapabilityBridge` must
not regain cohort or instinct methods.

### `impl/target/FactionTargetRules.java` and
`impl/target/FactionTargetPolicies.java`

Status: Migrated.

Current location: `impl/target/`.

Rule: target tag fallback, null-query handling, target eligibility policy
registration and ordering, and `FactionTargetEligibility` first-non-null
evaluation must stay in `impl/target/`. Public callers should continue using
`SparkFactionApi.canTarget(...)` and `SparkFactionApi.registerTargetEligibility(...)`;
internal callers must not route through a recreated capability bridge.

### `impl/shop/FactionShopAccessRules.java`

Status: Migrated.

Current location: `impl/shop/`.

Rule: Wathe killer-shop access must stay in `impl/shop/` and must only grant the
shop predicate from the explicit killer-feature capability. Do not broaden this
Module into economy, blackout, cohort, or instinct access.

### `impl/assignment/FactionAssignmentService.java` and
`impl/assignment/FactionSlotAllocator.java`

Status: Migrated.

Current location: `impl/assignment/`.

Rule: assignment phase ordering, desired slot calculation, shortage allocation,
role eligibility, and role selection ordering must stay in `impl/assignment/`.
Mixin modules may call `FactionAssignmentService` as thin Adapters, but must not
own assignment rules or move them back into the root `impl/` package.

### `impl/text/FactionRoundEndTextRules.java` and
`impl/text/FactionLetterTextRules.java`

Status: Migrated.

Current location: `impl/text/`.

Rule: translation keys, round-end phrase paths, letter faction-name rules, and
text color rules that are not tied to Minecraft rendering classes must stay in
`impl/text/`. Client and server mixin Adapters may call these Modules as thin
Adapters, but must not own text-key or letter-name rules.

### `impl/roundend/FactionWinService.java`,
`impl/roundend/FactionWinRules.java`,
`impl/roundend/FactionRoundEndStateRules.java`,
`impl/roundend/FactionWinnerCollector.java`,
`impl/roundend/FactionRoundEndRows.java`, and
`component/SparkFactionRoundEndComponent.java`

Status: Migrated.

Current location: `impl/roundend/` plus CCA storage in `component/`.

Rule: custom faction win hook registration, custom win-result aggregation,
custom round-end state lifecycle rules, winner collection, and Wathe round-end
row construction must stay in `impl/roundend/`. `SparkFactionRoundEndComponent`
must remain storage/sync/NBT only, with stable id `sparkfactionapi:round_end`
and stable keys `WinningFaction` / `Winners`. The transient pending-write flag is
server lifecycle state, not a persisted or downstream contract. Do not change the
custom win return path from
`CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.NEUTRAL)` to a Wathe
UUID winner path. Keep `CheckWinCondition.EVENT` delayed registration and keep
custom `FactionWinResult.block()` precedence over custom faction wins. A custom
round-end state may override Wathe rows only for the pending custom `NEUTRAL`
write that immediately follows `FactionWinService`; stale custom state must be
cleared before native team wins and explicit native neutral winner writes.

Follow-up direction: only revisit this area to split client display lookup from
text-key rules or to move CCA technical registration when that improves
Locality.

### `command/admin/SparkFactionAdminCommands.java`,
`command/admin/CooldownCommand.java`, `command/admin/TaskCommand.java`,
`command/admin/SanityCommand.java`, and
`command/admin/SparkFactionPermissions.java`

Status: Migrated.

Current location: `command/admin/`.

Rule: administrator command registration, cooldown mutation, task mutation,
sanity conversion, and permission nodes must stay separated inside
`command/admin/`. Preserve the `sparkfactionapi.command.admin` permission node,
default level `2`, Wathe task-completion semantics, and existing command
literals. Do not restore root `command/SparkFactionAdminCommands.java` or
`util/SparkFactionPermissions.java`.

### `command/settings/GameSettingsCommandRules.java`

Status: Migrated.

Current location: `command/settings/`.

Rule: Wathe game-settings role-list message rules, role toggle click-command
generation, enabled-state translation keys/colors, faction tag color/key
fallbacks, and special-role filtering must stay in this Module. The generated
toggle command must remain
`/wathe:gameSettings set enableRole <rolePath> <true|false>`. The root
`impl/GameSettingsCommandRules.java` Module must not be restored. The
`GameSettingsCommandMixin` Adapter may locate the Wathe Seam, obtain
`GameWorldComponent`, delegate to this Module, send the message, and set the
return value; it must not regain role-list text helpers.

### `net/version/VersionProtocol.java`,
`net/version/ServerVersionHandshake.java`, and
`client/net/version/ClientVersionHandshake.java`

Status: Migrated.

Current location: `net/version/` plus `client/net/version/`.

Rule: login channel id, local-version discovery, packet read/write behavior,
compatibility checks, missing-client messages, mismatch messages, and proxy-safe
unanswered-query behavior must stay in `VersionProtocol`. Server and client
Handshake Adapters must not duplicate protocol constants or message semantics.

### Mixed-responsibility mixins

Status: Migrated With Watch-Only Guardrails.

Migrated split:

- `mixin/MurderGameModeMixin.java`: `assignRolesAndGetKillerCount`
  assignment Adapter only.
- `mixin/MurderGameModePassiveMoneyMixin.java`: `tickServerGameLoop`
  passive-money Adapter only.

Rule: `MurderGameModeMixin` must not regain passive-money tick behavior, and
`MurderGameModePassiveMoneyMixin` must not gain assignment behavior. Keep
mixins thin Adapters and move behavior into domain Modules. Do not split
`GameSettingsCommandMixin` further unless a concrete bug proves that its
diagnostic logging, context lookup, and delegation cannot safely remain in one
Wathe command Adapter.
