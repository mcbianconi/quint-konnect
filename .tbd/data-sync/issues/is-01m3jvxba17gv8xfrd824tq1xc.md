---
type: is
id: is-01m3jvxba17gv8xfrd824tq1xc
title: Remove Runner.runTest and -Pquint.parallelism
kind: chore
status: open
priority: 3
version: 2
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
labels:
  - roadmap
  - assessment
dependencies:
  - type: blocks
    target: is-01m3js23q605rb3fyhzfjc8gnf
parent_id: is-01m3jvwgzvywep10dcef17jwd3
created_at: 2026-09-28T02:01:13.793Z
updated_at: 2026-09-28T02:01:20.654Z
---
Spec section B6 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
Problem: no generated code calls Runner.runTest (0.1.0 and 0.2.0 generators call traceReplays). -Pquint.parallelism only affects that path (ReplayRunner.kt:30,99), so for users it does nothing.
Scope: remove Runner, the thread pool, PARALLELISM_* properties (core + gradle-plugin), and the batch-path buffer logic in ConsoleReplayListener. First decide (write it here) whether ReplayRunner.runTest stays, sequential, for hand-written callers. Move integration-tests users (e.g. UnsafeCounterInvariantTest) to traceReplays.
Breaking change: update core.api and gradle-plugin.api; commit with a BREAKING CHANGE: footer. Remove the options from README, AGENTS.md and skills/quint-konnect/.
