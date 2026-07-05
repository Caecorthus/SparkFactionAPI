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

   The approval request must be specific enough to protect the old architecture.
   A module-level label is not enough by itself. For each proposed board of
   work, include:

   - Board: the named architecture area being changed.
   - Reason: the friction that makes the old module shape unsafe or costly to
     keep.
   - Old code scope: exact packages, files, and existing methods/helpers that
     will be moved, deleted, renamed, or rewritten.
   - New Module shape: the proposed package/module name and its intended
     Interface, including allowed responsibilities.
   - Forbidden scope: files, methods, policies, public Interface semantics,
     ordering, fallback behavior, and downstream contracts that must not
     change.
   - Behavior invariants: null behavior, fallback behavior, ordering, priority,
     role/faction isolation, and other semantics that must be preserved.
   - Downstream impact: whether downstream mods must change imports,
     registrations, capabilities, tests, or release expectations.
   - Verification plan: exact local tests, build commands, static checks, and
     downstream searches/checks.

   If any of these items are unknown, say so and perform a read-only review
   before requesting approval. Do not fill gaps by making assumptions during
   implementation.

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
    vision/
    target/
    roundend/
    text/

  mixin/
    thin Wathe Adapters only

  component/
    Cardinal Components registration Adapters only

  command/
    admin/
    settings/

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
- `impl/vision/`: instinct highlight decisions, cohort display decisions, and
  client highlight priority conversion rules.
- `impl/target/`: target tags and `canTarget` decisions.
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
command/settings/
  GameSettingsCommandRules.java
```

The aggregate command module registers command trees. Testable parsing,
selection, conversion, and feedback rules should live in small internal rule
Modules.

Wathe game-settings list-role text, click-command generation, enabled-state
labels, and faction tag display rules live in `command/settings/`. The mixin
Adapter may delegate there, but must not own those text rules.

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

## Architecture Closure

There is no active architecture-refactor backlog. The current target shape is
good enough to protect Locality without turning the project into an endless
Module-splitting exercise.

### Stop Line

Do not split, move, rename, or delete Modules just because a finer shape is
possible. Future architecture work requires an owner-approved board and at
least one of these concrete triggers:

- A bug, crash, or compatibility issue proves the current Module shape caused
  drift.
- A new feature needs behavior at an existing Seam and would otherwise add logic
  to the wrong Module.
- Tests, build output, or this document show the implementation has diverged
  from the rules below.

Absent one of those triggers, keep the architecture stable and make local,
task-scoped changes only.

### Legacy And Migration Logs

Closed-board history and migrated, retired, or watch-only module details now
live in [ARCHITECTURE LOGS.md](<ARCHITECTURE LOGS.md>).

Read that file before touching retired, migrated, watch-only, or former legacy
modules. It records constraints for old seams only; all approval and
verification gates remain in this file.

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
