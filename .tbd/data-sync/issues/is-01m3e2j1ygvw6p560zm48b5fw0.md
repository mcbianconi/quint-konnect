---
type: is
id: is-01m3e2j1ygvw6p560zm48b5fw0
title: Eval the skill on a scratch Kotlin project
kind: task
status: closed
priority: 2
version: 6
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:11.631Z
updated_at: 2026-09-26T20:44:39.687Z
closed_at: 2026-09-26T20:44:39.687Z
close_reason: "Ran skills/quint-konnect on a scratch bounded-counter Kotlin/Quint project (not from repo examples), acting as an agent with only SKILL.md + references. Happy path passed first try after fixing a self-inflicted build.gradle.kts repositories{} block; negative case (clamp bug) reproduced deterministically via the documented seed/QUINT_VERBOSE flow. Fixed 4 doc gaps in skills/quint-konnect/**: (1) no mavenLocal() wiring snippet + the repositories{}-vs-dependencyResolutionManagement footgun, (2) missing FQNs for TypedState/DriverConfig/QuintIgnore, (3) Gradle's default exceptionFormat=SHORT hides the AssertionError message itself, not just stderr, (4) QUINT_SEED alone doesn't reproduce a failure found under a -Pquint.maxSteps override. Filed qk-144o (bug: reproduce line should print maxSamples/maxSteps overrides) and qk-73yw (feature: plugin should configure testLogging itself). Trigger-description judged manually with 8 should-trigger/8 should-not-trigger prompts (no automated harness run from this subagent) -- description holds up on all 16, no change made. Commit zqm on branch skill-eval-fixes."
resolution: null
duplicate_of: null
---
Fresh agent with only the skill wires a driver + state for a small spec until ./gradlew test passes with quint 0.32.0 (use includeBuild or publishToMavenLocal before 0.1.0). Negative case: buggy driver (commit 7e95ae3) failure output and seed are read correctly. Use anthropic-skills:skill-creator to run evals and tune the description for triggering.
