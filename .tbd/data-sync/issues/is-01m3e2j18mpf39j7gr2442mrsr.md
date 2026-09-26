---
type: is
id: is-01m3e2j18mpf39j7gr2442mrsr
title: Draft quint-konnect agent skill
kind: feature
status: open
priority: 2
version: 5
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j1fpss05vf4k0v0e9hwr
  - type: blocks
    target: is-01m3e2j1pz2js33v1dqzd8ksp5
  - type: blocks
    target: is-01m3e2j1ygvw6p560zm48b5fw0
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:10.932Z
updated_at: 2026-09-26T06:49:37.860Z
---
User-facing skill at skills/quint-konnect/ (separate from repo dev skills in .claude/skills/). SKILL.md workflow: prereqs (quint 0.32.0), Gradle+KSP deps with <VERSION> placeholder, driver mapping (@QuintRun/@QuintTest, @QuintAction, nullable nondet picks) using the driver shape from qk-vmya (no generatedStep), TypedState, run/debug (QUINT_SEED, QUINT_VERBOSE, AssertionError contract). references/types.md holds the Quint->Kotlin type table. Known-limitations section lists the limitations still open when drafting (qk-kl73, qk-mmbq, qk-9sdm, qk-oqok are fixed). Suspend actions (qk-33ky) and Driver<S> (qk-hpzp) update the skill when they land. Worked example via GitHub URLs to example/. Defer spec authoring to quint-llm-kit's quint-lang skill. Also add docs/decisions/agent-skill.md + README line. Plan: ~/.claude/plans/should-the-project-publish-cheeky-balloon.md
