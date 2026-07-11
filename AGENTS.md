# Advanced Spark

## Repositories
- https://github.com/Caecorthus/SparkTraits (By default, check this repository first; if it hasn't been cloned locally, use GitHub/gh to retrieve the context)
- https://github.com/XruiDD/TrainMurderMystery (Spark-ver wathe)
- https://github.com/XruiDD/NoellesRoles (Spark-ver NoellesRoles, a role expansion add-on mod for wathe)
- https://github.com/Caecorthus/SparkFactionAPI (Faction API)
- https://github.com/Caecorthus/SparkWitch (Spark-ver Factions & Roles addons)
- https://github.com/Caecorthus/SparkStrength (Spark-ver roles buff)
- https://github.com/Caecorthus/SparkAssist (Client-side assist mod)

## Skills
- Use this skill in your and your subagents' workflow
	- [$using-superpowers](/Users/kricy/.codex/skills/using-superpowers/SKILL.md)

## Coding

### Architecture
- Before changing code, read [ARCHITECTURE.md](ARCHITECTURE.md) and follow its Mandatory Rules.
- The architecture document is binding for future agents. If a change touches a Legacy / Pending Refactor / Deletion Candidate area, explain the reason, scope, impact, and verification plan before editing, then wait for explicit owner approval.
- Do not treat target architecture notes as automatic permission to delete, move, rename, split, merge, or rewrite existing modules.

### Cross-Repository Check
- Inspect the owning repository and every direct consumer of the contract being
  changed. Do not mechanically read every Spark repository when the change is
  local and no cross-mod seam is involved.
- For public API, policy, capability, version, or effective-faction changes,
  search SparkWitch and SparkTraits for consumers. SparkStrength and SparkAssist
  are checked only when code or metadata shows a relevant dependency.
- Prove that unrelated roles, factions, and traits remain outside the changed
  predicate and behavior path.
- Use Java 21 for focused tests, `verifyArchitecture`, and builds. Follow the
  risk-based test rules in `ARCHITECTURE.md`; never add production-only test
  hooks.

### Subagents
- You're the coordinator/leader between the subagents.
- Please use subagents. You can code with them together at the same time.
- Create multiple subagents for multiple purposes. Create as many as you needed.
	- Do NOT overload one subagent. Create multiple subagents to divide the tasks.
- Use English as the main language between you and your subagents.

### Annotations
- Add concise English and Chinese comments for non-obvious public API, mixin,
  Wathe seam, cross-mod, or migration semantics. Do not duplicate self-evident
  code in comments.
