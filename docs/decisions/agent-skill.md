---
name: agent-skill
date: 2026-09-26
---

quint-konnect publishes a user-facing AI agent skill at `skills/quint-konnect/`, kept separate
from this repo's own dev skills under `.claude/skills/`. It teaches an agent to wire a Kotlin
driver to an existing Quint spec (Gradle/KSP setup, `@QuintRun`/`@QuintTest`/`@QuintAction`,
`TypedState`, reading `QUINT_SEED`/`QUINT_VERBOSE` failures); it defers spec authoring and the
Quint language itself to quint-llm-kit's `quint-lang` skill (github.com/quint-co/quint-llm-kit)
instead of duplicating it.

**Why:** Wiring a Kotlin driver to a spec is easy to get wrong (the KSP generated source dir,
`@QuintAction` name matching, nullable nondet picks, `TypedState`, the Quint-to-Kotlin type
table, seed/verbose debugging) and nothing upstream teaches it for Kotlin — quint-llm-kit's
Quint Connect tooling is Rust-only. Drafting it now, ahead of the 0.1.0 release, keeps it in
sync with the API as Phase 3 (developer experience) lands, instead of writing it once at the
end against a frozen snapshot; publishing/distribution (a Claude Code marketplace entry,
`npx skills`) is a separate concern (docs/decisions/agent-skill-plugin.md), released once
0.1.0 shipped to Maven Central (qk-q6aq).

**How to apply:** A change to `annotations`, `Driver`, `TypedState`, `DriverConfig`, the
Gradle plugin, or env vars/Gradle properties must update `skills/quint-konnect/` in the same
change (the rule is also in CLAUDE.md and AGENTS.md). `skills/quint-konnect/references/types.md`
is the only copy of the Quint-to-Kotlin type table; README and CLAUDE.md link to it and
`TypesMdDocTest` decodes every row's example. Packaging as a Claude
Code plugin/marketplace entry and replacing the `<VERSION>` placeholder with real coordinates
are separate beads (qk-8o8s, qk-q6aq), not part of this decision.
