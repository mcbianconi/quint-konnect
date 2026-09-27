---
type: is
id: is-01m3j7vn4yap7e5egmd816wjqy
title: Move internal regression tests from example/ into a new :integration-tests module
kind: task
status: closed
priority: 3
version: 5
delegate: claude-code@vm
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3j7vnc9vr73kchj9xd35bcc
parent_id: is-01m3j7vmxf3e87tr20a324y2s9
hold: null
hold_until: null
created_at: 2026-09-27T20:10:46.813Z
updated_at: 2026-09-27T20:33:36.351Z
started_at: 2026-09-27T20:29:39.284Z
closed_at: 2026-09-27T20:33:36.351Z
close_reason: Done in 74a3931 on claude/example-standalone-qk-03a3
resolution: null
duplicate_of: null
---
Create `:integration-tests` in the root build (`settings.gradle.kts` include), unpublished,
wired like `example/build.gradle.kts` is today (`kspTest(project(":ksp"))`,
`testImplementation(project(":core"))`, the hand-rolled `quintIr` Exec tasks, `ksp.arg`,
`quintkonnect.projectDir` system property). Copy that build file as the starting point.

Per file under `example/src/test/kotlin/io/github/mcbianconi/quintkonnect/example/`:

| File(s) | Action |
|---|---|
| `escaping/*` + `src/test/resources/escaping/counter$1.qnt` | Move to `:integration-tests` (qk-gu38 fixture). Keep it out of that module's IR specs as today. |
| `buggy/BuggyRockPaperScissorsShrinkTest.kt` | Move (it asserts exact shrink output for seed 42 via `ReplayRunner` + hand-set `quintkonnect.shrink`). |
| `replay/ReplayFixtureTest.kt` + `replay/counter-trace.itf.json` | Move (constructs `ItfFileTraceSource` directly). Needs a copy of `quinttest/CounterDriver` or its own driver. |
| `invariants/UnsafeCounterInvariantTest.kt`, `UnsafeCounterDriver.kt`, `unsafe_counter.qnt` | Move (raw `Runner.runTest` + `RunConfig`, asserts the violation message). |
| `buggy/BuggyRockPaperScissorsTest.kt`, `partialstate/PartialStateRpsTest.kt`, `projection/BuggyWarehouseTest.kt` | Keep in `example/`: they show "the harness catches a bug" / partial checks, which users do. qk-14ek touches them. |

Acceptance:
- No loss of real-quint coverage: list in the PR which behaviours had e2e coverage only in
  `example/` and where each lives now.
- `./gradlew :integration-tests:test` and `./gradlew :example:build` pass (quint in PATH).
- Update AGENTS.md's Build & Test command list and the "not `example`" notes on explicit API /
  publishing to include `integration-tests`; `quintkonnect.publish.gradle.kts`'s comment too.
