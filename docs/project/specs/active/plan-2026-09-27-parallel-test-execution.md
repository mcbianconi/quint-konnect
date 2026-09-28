---
title: Parallel test execution for this repo's own build
description: Run quint-konnect's own test tasks concurrently without flaky cross-test interference
---
# Feature: Parallel test execution for this repo's own build

**Date:** 2026-09-27 (last updated 2026-09-27)

**Author:** Murillo Cesar Bianconi

**Status:** Draft

## Overview

Cut the wall-clock time of `./gradlew build` (locally and in `ci.yml`) by running this repo's
test tasks in parallel: modules at the same time, and test classes split across several test
JVMs. In-JVM JUnit concurrency is a second, conditional step. It happens only where the
baseline shows it saves time and only after the shared-state hazards below are removed.

## Goals

- `./gradlew build` runs independent modules' tasks concurrently.
- Every `Test` task in the root build (`test` in each module, `gradle-plugin:functionalTest`)
  can fork more than one test JVM, sized from the machine's CPU count.
- The suite stays deterministic: several consecutive `--rerun-tasks` runs pass with no new
  flaky failures.
- The measured time saved is recorded in this spec, per `Test` task, against a baseline.

## Non-Goals

- User-facing parallelism. The Gradle plugin, `ReplayRunner`, `-Pquint.parallelism` and the
  generated `@Execution(CONCURRENT)` `@TestFactory` (qk-t281, qk-voya) are out of scope. No
  change to `gradle-plugin/src/main` or `core/src/main`, so `skills/quint-konnect/` needs no
  update.
- The `example` build (`./gradlew -p example build`). It is a separate build with its own
  `settings.gradle.kts`, so root `gradle.properties` and `build-logic` don't reach it. It stays
  as it is. It can be revisited once the root build is done.
- Gradle build cache or configuration cache.

## Background

The root build has six modules (`annotations`, `itf`, `core`, `ksp`, `gradle-plugin`,
`integration-tests`). No parallelism is configured at any level today: root `gradle.properties`
only sets `org.gradle.jvmargs=-Xmx2g`, and no `Test` task sets `maxParallelForks` or ships a
`junit-platform.properties`.

The expected cost centres differ by module:

- `ksp`: kotlin-compile-testing compiles driver fixtures in-process. This is CPU-heavy and
  heap-heavy.
- `gradle-plugin:functionalTest`: TestKit starts Gradle daemons through `GradleRunner`.
- `integration-tests`: starts real `quint` (Node) processes, plus one `quintIr_*` `Exec` task
  per spec.
- `core`/`itf`: small, fast unit tests.

These are expectations, not measurements. Task 1 below replaces them with numbers.

### Shared JVM state in the tests

Main code reads `quintkonnect.*` system properties at call time, through default parameters:
`defaultTraceSource` (`replay`, `tracesDir`), `writeFailureTrace` (`failuresDir`),
`resolveSpec` (`projectDir`), `maxSamplesOverride`/`maxStepsOverride`, `shrinkEnabled`,
`resolveParallelism`. The tests set some of them with `System.setProperty`:

| File | Writes |
| --- | --- |
| `core/.../GeneratorConfigTest.kt` | 6 |
| `core/.../ReplayRunnerTest.kt` | 8 |
| `core/.../ShrinkTest.kt` | 2 |
| `integration-tests/.../shrink/BuggyRockPaperScissorsShrinkTest.kt` | 2 |
| `ksp/.../ParallelDynamicTestExecutionTest.kt` | 2 |
| `ksp/.../QuintRunTestGeneratorTest.kt` | 2 |
| `ksp/.../QuintTestTestGeneratorTest.kt` | 2 |

System properties are global to the JVM. If a test sets `quintkonnect.replay`, any other test
running at the same time in that JVM that builds a `ReplayRunner` with the default trace source
is affected, whether or not that other test touches properties itself. Forked test JVMs run
their classes one after another, so this is only a hazard for in-JVM concurrency (Phase 2).

`ParallelDynamicTestExecutionTest` is the qk-voya regression guard. It runs a nested JUnit
`Launcher` with sleep-based timing and a `ConcurrencyRecorder` singleton. If the outer JVM
enables JUnit parallelism, the nested launcher can inherit that configuration and the test can
pass for the wrong reason.

## Design

### Approach

Phase 1 isolates tests by process, not by locks, so no test code changes:

1. `org.gradle.parallel=true` in root `gradle.properties`. Decoupled projects' tasks run
   concurrently, including `integration-tests`' `quintIr_*` `Exec` tasks.
2. In `build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`, which every root module
   applies directly or through `quintkonnect.library`:

   ```kotlin
   // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:maxParallelForks
   tasks.withType<Test>().configureEach {
       maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, MAX_FORKS)
   }
   ```

   `withType<Test>()` also reaches `gradle-plugin`'s `functionalTest`, which `tasks.test {}`
   would not. `MAX_FORKS` and the divisor come from the baseline (Task 1). A fixed number tuned
   on one developer machine is not acceptable. `ubuntu-latest` has 4 vCPUs, and module-level
   parallelism already competes for them.
3. Resource ceiling. `org.gradle.jvmargs` sizes only the daemon. Each test fork gets its own
   heap (Gradle's default is 512m). Set `maxHeapSize` explicitly on `ksp`'s `test` task if the
   baseline shows memory pressure there. Record peak fork count × heap against the runner's
   memory.
4. `gradle-plugin:functionalTest` stays at `maxParallelForks = 1` unless it is confirmed that
   concurrent `GradleRunner`s sharing the default test-kit directory are safe. The Gradle docs
   checked for this spec don't say either way. Options if they are not safe: keep 1 fork, or
   give each fork its own `withTestKitDir`.

Phase 2 (conditional) adds in-JVM JUnit concurrency only for a module whose `test` task is
still the critical path after Phase 1, and only after that module's shared-state hazards are
removed:

- Prefer removing the hazard over locking it. The main-code functions already take the
  property value as a parameter (`resolveSpec(spec, projectDir)`,
  `defaultTraceSource(testName, replayPath, tracesDir)`, `resolveSeed(..., override, envSeed)`,
  `maxSamplesOverride(value)`), so tests can pass values directly instead of calling
  `System.setProperty`.
- Where a test must go through the real property, mark writers
  `@ResourceLock(Resources.SYSTEM_PROPERTIES, mode = READ_WRITE)`. Also mark every test that
  reaches a property-reading default path with `mode = READ`. Locking only the writers is not
  enough.
- `ParallelDynamicTestExecutionTest` gets `@Isolated`. Its nested launcher must set
  `junit.jupiter.execution.parallel.*` explicitly and not inherit them from the outer JVM.
- Configure per module in `src/test/resources/junit-platform.properties`, with a reference
  comment:

  ```properties
  # https://docs.junit.org/current/user-guide/#writing-tests-parallel-execution
  junit.jupiter.execution.parallel.enabled = true
  junit.jupiter.execution.parallel.mode.default = same_thread
  junit.jupiter.execution.parallel.mode.classes.default = concurrent
  ```

  Classes run concurrently and methods within a class stay sequential. That keeps the change
  small for classes that share `companion object` compilation results (`ksp` tests).

### Components

- `gradle.properties` (root)
- `build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`
- `ksp/build.gradle.kts` (heap, if needed)
- Phase 2 only: test sources in the affected module(s), their
  `src/test/resources/junit-platform.properties`

### API Changes

None.

## Implementation Plan

### Phase 1: Process-level parallelism

- [ ] Baseline: run `./gradlew build --rerun-tasks` on the current `main`. Record total time
  and each `Test` task's duration (Gradle MCP `query_build`) in this spec's Results section.
  Do this locally and once on CI.
- [ ] Add `org.gradle.parallel=true` to root `gradle.properties`, with a reference comment.
- [ ] Add CPU-derived `maxParallelForks` for every `Test` task in
  `quintkonnect.kotlin-jvm.gradle.kts`. Pin `gradle-plugin:functionalTest` to 1 unless
  concurrent TestKit use is confirmed safe.
- [ ] Set `maxHeapSize` for `ksp`'s `test` task if the baseline shows memory pressure.
- [ ] Re-measure locally and on CI. Record the new durations next to the baseline.
- [ ] Stability: 5 consecutive local `./gradlew build --rerun-tasks` runs with no failures, then
  green CI.

### Phase 2: In-JVM JUnit concurrency (only if Phase 1 results justify it)

- [ ] Choose the module(s) from the Phase 1 numbers. Skip this phase entirely if no module's
  `test` task is the critical path.
- [ ] Replace `System.setProperty` in that module's tests with the existing override
  parameters. Where that isn't possible, add `@ResourceLock(SYSTEM_PROPERTIES)` in READ/READ_WRITE
  mode on writers and readers.
- [ ] `ksp` only: `@Isolated` on `ParallelDynamicTestExecutionTest`, and explicit parallel
  configuration on its nested launcher.
- [ ] Add the module's `junit-platform.properties`.
- [ ] Re-measure and run the stability check again.

## Testing Strategy

- The stability check (5 consecutive clean `--rerun-tasks` runs plus green CI) is the gate for
  each phase. Concurrency failures are intermittent, so a single green run is not evidence.
- qk-voya guard stays meaningful. With Phase 2 applied to `ksp`, temporarily remove
  `@Execution(ExecutionMode.CONCURRENT)` from `QuintRunTestGenerator` and confirm
  `ParallelDynamicTestExecutionTest` fails. Restore it afterwards.
- Confirm `example` still builds unchanged (`./gradlew -p example build`).

## Rollout Plan

One PR per phase, through the normal CI. No release is involved: nothing published changes.

## Open Questions

- Is concurrent `GradleRunner` use with the default test-kit directory safe on Gradle 9.8? If
  that can't be confirmed, `functionalTest` stays at 1 fork.
- Does `example` warrant the same treatment later? It would need its own `gradle.properties`.
  It also runs through the published-plugin path, so a JUnit config there would dogfood the
  README's "Running traces in parallel" instructions.

## Results

### Baseline (qk-ef4s, 2026-09-27)

Local: `build --rerun-tasks`, macOS, 8 cores / 16 GB, Gradle MCP build `b-16`.

| Test task | Duration | Test classes |
| --- | --- | --- |
| `:itf:test` | 1.061s | 4 |
| `:core:test` | 1.452s | 15 |
| `:ksp:test` | 24.65s | 13 |
| `:gradle-plugin:test` | 5.324s | 6 |
| `:gradle-plugin:functionalTest` | 24.73s | 3 |
| `:integration-tests:test` | 3.088s | 3 |
| **Total build** | **1m 23s** | — |

`:ksp:test` and `:gradle-plugin:functionalTest` are tied for critical path, each about 30% of
total wall time; every other `Test` task is small by comparison. 389 tests passed, 0 failed.

Memory: no evidence of pressure on `:ksp:test`. It ran as a single fork on the default 512m
heap; the console has no `OutOfMemory`/GC-overhead output, and its 82 tests each finished in
under 1.3s (cumulative 24.65s is kotlin-compile-testing/KSP2 compilation cost per test, not GC
thrashing).

CI: `./gradlew build` (no `--rerun-tasks`, but a fresh checkout each run), `ubuntu-latest`
(4 vCPU), the "Build" step of the 3 most recent green `main` runs:

| Run | Duration |
| --- | --- |
| 36368608597 | 2m48s |
| 36367574782 | 2m45s |
| 36360182308 | 2m51s |
| **Average** | **~2m48s (168s)** |

### Post-change (qk-f3fi, 2026-09-27)

(Filled in after Phase 1's config changes: re-measured durations next to the baseline above.)

## References

- JUnit parallel execution:
  https://docs.junit.org/current/user-guide/#writing-tests-parallel-execution
- Gradle `Test.maxParallelForks`:
  https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html
- Gradle parallel execution:
  https://docs.gradle.org/current/userguide/performance.html#parallel_execution
- qk-t281 (run traces in parallel), qk-voya (parallel dynamic tests README fix)
