# SparkFactionAPI Architecture Constitution

This document is mandatory for all future agents working in this repository.
It defines the target architecture and the governance rules for changing
existing modules. It is not a blanket authorization to refactor old code.

## Mandatory Rules

1. Read this file before changing code.

2. Protect `src/main/java/dev/caecorthus/sparkfactionapi/api/` as the stable
   public Interface for downstream mods.

   - Default change mode is backward-compatible extension.
   - Do not change existing method semantics, record fields, policy fallback
     behavior, registration ordering, or null-handling unless the task
     explicitly approves a breaking change.
   - A breaking public Interface change requires a reason, scope, downstream
     impact list, version plan, and verification plan before editing.

3. Existing legacy modules are not free to rewrite.

   Before deleting, moving, renaming, splitting, merging, or substantially
   changing any existing source module, first provide:

   - Reason: why the old module cannot stay as-is.
   - Scope: exact packages and files to be touched.
   - Impact: behavior, public Interface, downstream mods, tests, and release
     risk.
   - Verification plan: local tests, build commands, and downstream checks.

   Wait for explicit owner approval before making that change.

4. `Legacy`, `Pending Refactor`, and `Deletion Candidate` labels are direction
   markers only. They are not automatic permission to edit or delete.

5. New behavior must not be added to legacy catch-all modules. If a task touches
   one of those modules and the owner approves a change, move the behavior into
   the owning target Module instead of making the catch-all deeper.

6. `mixin/` classes are thin Adapters at Wathe Seams.

   A mixin may:

   - Locate the injection point.
   - Read the minimum required context.
   - Delegate to a domain Implementation module.

   A mixin must not own gameplay rules, policy ordering, text rules, economy
   rules, targeting rules, or round-end rules.

7. Every meaningful Seam must preserve unrelated roles, factions, and traits.
   If a change is about one faction, role, trait, command, or policy, prove the
   change does not broaden into unrelated behavior.

8. Comments must be English and Chinese when they explain:

   - Public Interface semantics.
   - Wathe Seam behavior.
   - Mixin injection reasons.
   - Cross-mod compatibility rules.
   - Legacy retention or migration reasons.

   Do not add noise comments to self-explanatory code.

9. Tests should cross the same Interface as callers. Prefer testing domain
   Modules through their real Interface instead of reaching into private helper
   details. Package-private pure rules are allowed when they preserve Locality.

10. If a change triggers downstream migration, create or update
    `DOWNSTREAM_MIGRATION_NOTES.md` in the repo root.

    Triggering changes include:

    - Any `api/` public Interface change.
    - Any `FactionCapabilities` field or semantic change.
    - Any policy ordering, priority, null, fallback, or registration behavior
      change.
    - Any login version handshake protocol or rejection behavior change.
    - Any change requiring downstream import, registration, capability, or test
      updates.

    Pure internal movement does not require downstream notes if public behavior
    stays identical.

## Target Architecture

Only `api/` is the public downstream Interface. All other packages are internal
Implementation or Adapter modules unless this document says otherwise.

```text
src/main/java/dev/caecorthus/sparkfactionapi/
  SparkFactionApiMod.java

  api/
    SparkFactionApi.java
    FactionDefinition.java
    FactionRoleDefinition.java
    FactionCapabilities.java
    Faction*Policy.java
    Faction*Context.java
    Faction*Result.java
    FactionIds.java

  impl/
    registry/
    assignment/
    capability/
    economy/
    gun/
    blackout/
    instinct/
    targeting/
    roundend/
    text/

  mixin/
    thin Wathe Adapters only

  component/
    Cardinal Components registration Adapters only

  command/
    admin/

  net/
    version/

src/client/java/dev/caecorthus/sparkfactionapi/client/
  SparkFactionApiClient.java
  net/version/
  mixin/
```

### `api/`

The `api/` package is the stable public Interface for SparkWitch,
SparkStrength, SparkTraits, and any future add-on.

Keep state declarations and behavior Seams separate:

- `FactionCapabilities` describes faction capability state.
- `Faction*Policy` modules describe behavior override Seams.
- `SparkFactionApi` is the public facade. It may delegate internally, but it
  must not expose internal Implementation classes.

### `impl/`

The root `impl/` package is a legacy transition area. Do not add new files to
the root of `impl/` without explicit approval.

New or migrated internal code must live in a domain subpackage:

- `impl/registry/`: faction registration, role registration, Wathe role mapping,
  native bucket shielding, and effective faction resolver registration.
- `impl/assignment/`: assignment phases, desired slot calculation, shortage
  allocation, and role selection ordering.
- `impl/capability/`: basic capability resolution and shared capability queries.
- `impl/economy/`: passive money, kill rewards, task rewards, and economy policy
  evaluation.
- `impl/gun/`: gun punishment, innocent-shot semantics, and revolver
  consumption.
- `impl/blackout/`: blackout immunity, blackout cooldown sharing, and blackout
  policy evaluation.
- `impl/instinct/`: instinct highlight decisions, cohort display decisions, and
  client highlight priority conversion rules.
- `impl/targeting/`: target tags and `canTarget` decisions.
- `impl/roundend/`: custom faction win resolution, winner collection, round-end
  state semantics, and server/client round-end display coordination.
- `impl/text/`: translation key and display text rules that are not tied to
  Minecraft rendering classes.

### `mixin/`

Mixin modules are Adapters. They should be small enough that deleting the mixin
would move injection knowledge, not gameplay rules.

If a mixin currently contains multiple responsibilities, do not add a third.
After approval, extract each responsibility into the owning domain Module.

### `component/`

`component/` owns Cardinal Components registration and technical wiring only.
Round-end state semantics belong to `impl/roundend/`.

### `command/`

Admin command structure should converge toward:

```text
command/admin/
  SparkFactionAdminCommands.java
  CooldownCommand.java
  TaskCommand.java
  SanityCommand.java
  SparkFactionPermissions.java
```

The aggregate command module registers command trees. Testable parsing,
selection, conversion, and feedback rules should live in small package-private
rule modules.

### `net/`

Version networking should converge toward:

```text
net/version/
  VersionProtocol.java
  ServerVersionHandshake.java

client/net/version/
  ClientVersionHandshake.java
```

Protocol constants, packet read/write behavior, compatibility checks, and
disconnect messages must be shared. Server and client Adapters must not each
invent their own channel or message semantics.

## Legacy Register

This register records known non-target modules. It does not authorize edits.

### `impl/FactionCapabilityBridge.java`

Status: Deletion Candidate.

Target direction: remove this module after approved migration.

Reason: it combines capability, economy, gun, blackout, cohort, instinct,
targeting, policy iteration, and effective-faction helpers. That creates poor
Locality and makes future behavior changes hard to audit.

Rule: do not add new behavior to this module. When a task is approved to touch
one of its responsibilities, move that responsibility into the owning domain
Module and delete the bridge method once no local caller remains.

### `impl/FactionRegistryImpl.java`

Status: Pending Refactor.

Target direction: split registry state, legacy faction bootstrapping, role
registration, resolver registration, and policy list ownership into
`impl/registry/` plus the relevant domain Modules.

### `impl/FactionCompatibilityEvents.java`

Status: Pending Refactor.

Target direction: keep only event registration aggregation, or move event
Adapter registration into the owning domain Modules when that improves
Locality.

### `impl/FactionAssignmentService.java` and `impl/FactionSlotAllocator.java`

Status: Pending Move.

Target direction: `impl/assignment/`.

### `impl/FactionWinService.java`, `impl/FactionRoundEndTextRules.java`, and
`component/SparkFactionRoundEndComponent.java`

Status: Pending Refactor.

Target direction: round-end semantics move to `impl/roundend/`; CCA technical
registration remains in `component/`.

### `command/SparkFactionAdminCommands.java` and `util/SparkFactionPermissions.java`

Status: Pending Refactor.

Target direction: `command/admin/`, with registration separated from cooldown,
task, sanity, and permission rules.

### `net/SparkFactionVersionHandshake.java` and `net/SparkFactionVersionCheck.java`

Status: Pending Refactor.

Target direction: shared `net/version/VersionProtocol` plus server/client
Handshake Adapters.

### Mixed-responsibility mixins

Status: Pending Refactor.

Known examples:

- `mixin/GameFunctionsMixin.java`
- `mixin/MurderGameModeMixin.java`
- `mixin/GameSettingsCommandMixin.java`

Target direction: make mixins thin Adapters and move behavior into domain
Modules.

## Approval Template For Legacy Changes

Use this before modifying old architecture:

```text
Reason:
Scope:
Impact:
Verification:
Downstream notes needed: yes/no
```

No approval, no edit.

## Verification Expectations

For documentation-only changes:

```bash
./gradlew test
```

For code changes:

```bash
./gradlew clean test
./gradlew build
git diff --check
```

If public Interface behavior changes, also identify affected downstream repos
and run the smallest meaningful downstream compile/test/build checks.
