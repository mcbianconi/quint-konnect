# Decisions

Project decisions and standing constraints that aren't derivable from the code, tests,
KDoc, other docs (README, CLAUDE.md, AGENTS.md, skills), or git history. One file per
decision. Keep entries short: the decision, why, and how it should shape future work.

Before adding an entry, check whether the fact is already visible elsewhere (a code
comment, an error message, a config file). If so, put the decision there instead — a
comment next to the code it governs is easier to keep in sync than a separate file.

- [No Windows support](no-windows-support.md) — Windows isn't a target platform
- [License](license.md) — Apache-2.0, copyright holder "Murillo Cesar Bianconi"
- [ITF module](itf-module.md) — why ITF parsing/normalization is its own `:itf` module instead of living in `core`
- [ITF collection mapping](itf-collection-mapping.md) — `Set<T>` for sets, `Map<List<Long>, V>` for tuple-keyed maps, and why not the more obvious `List<T>`/`Pair`
- [ITF Option and BigInt mapping](itf-option-and-bigint.md) — the MBT harness's unconditional `Option` unwrap vs. the descriptor-driven one
- [quint test needs a nondet path](quint-test-needs-nondet-path.md) — `@QuintTest` replay requires the spec to model the action taken as a sum type and `nondetPath` to point at it
- [Gradle plugin distribution](gradle-plugin-distribution.md) — `quint-konnect-gradle-plugin` publishes to Maven Central with its marker artifact, not the Gradle Plugin Portal
- [Agent skill](agent-skill.md) — a user-facing skill at `skills/quint-konnect/`, separate from this repo's dev skills, defers spec authoring to quint-llm-kit
- [Agent skill plugin packaging](agent-skill-plugin.md) — root-rooted `.claude-plugin/` (source `"./"`) so the skill doesn't move; `npx skills add` still surfaces the dev skills, plugin.json version pinned to the library version
- [Quint IR source](quint-ir-source.md) — `quintIr` runs `quint typecheck --out`, not `quint compile`, and collects an action's `nondet` params transitively through the actions it calls
