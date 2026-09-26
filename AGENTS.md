# Project Instructions for AI Agents

This file provides instructions and context for AI coding agents working on this project.

<!-- BEGIN TBD INTEGRATION format=f08 surface=agents-md -->
## tbd

This repository uses **tbd** for git-native issue tracking (beads), spec-driven
planning, and on-demand engineering guidelines.
As the agent, you operate tbd on the user’s behalf: translate their requests into tbd
actions rather than telling them to run commands.

- Run `tbd prime` to load current project state and the full tbd workflow.
- Run `tbd skill` for the complete reusable tbd skill instructions.
- Run `tbd shortcut --list` and `tbd guidelines --list` for on-demand resources.
- Track all work as beads: `tbd create`, `tbd ready`, `tbd start`, `tbd close`, and
  `tbd sync`.
- Before editing a bead, pull and re-read it, run `tbd start <id>`, then run `tbd sync`
  so other replicas can see the claim.

<!-- END TBD INTEGRATION -->

## Build & Test

Requires JDK 21 and `quint` in `PATH` (CI pins `@informalsystems/quint@0.32.0`, see the
comment on that line in `.github/workflows/ci.yml`).

```bash
./gradlew :annotations:build         # Build annotation declarations
./gradlew :itf:test                  # Run ITF parsing/normalization unit tests
./gradlew :core:test                 # Run core unit tests (no quint CLI required)
./gradlew :ksp:build                 # Build KSP processor
./gradlew :example:build             # Build example + run end-to-end test (requires quint in PATH)
./gradlew build                      # Build all modules
```

Run a single test with `--tests`, e.g. `./gradlew :core:test --tests TraceGeneratorTest`.
`QUINT_VERBOSE=1`/`2` and `QUINT_SEED=<hex>` control logging and reproducibility (see
README.md's Environment variables section).

## Architecture Overview

Five modules, in dependency order:

- `annotations` — `@QuintRun`, `@QuintTest`, `@QuintAction` declarations only. No runtime
  dependency, so it stays on a driver's compile classpath without pulling in `core`.
- `itf` — ITF parsing and value normalization (`ItfValue`, `ItfTrace`/`parseTrace`,
  `ItfValueSerializer`, `ItfValue.decode`). JSON normalization is internal to the module.
- `core` — the runtime: `quint` CLI invocation and trace generation (`trace/`), step
  extraction (`Step.kt`), nondet pick decoding (`nondet/`), state comparison
  (`State.kt`), and the replay loop (`Runner.kt`).
- `ksp` — a KSP2 processor that reads `@QuintRun`/`@QuintTest`/`@QuintAction` on a driver
  class and generates a JUnit 5 test class plus a `generatedStep()` dispatcher
  (`ksp/generators/`).
- `example` — end-to-end examples: TicTacToe, rock-paper-scissors, a buggy driver the
  tests expect to fail, a `@QuintTest` counter, and a fixture for escaped names.

Data flow: a Quint spec is run through the `quint` CLI (`quint run --mbt` or
`quint test`) to produce ITF trace files; `TraceGenerator` invokes the CLI and parses the
output into `ItfTrace`/`ItfValue`; `Runner` replays each trace step against the driver
(generated `generatedStep()` dispatches to the right `@QuintAction` method, decoding
nondet picks into method parameters) and, when the driver provides a `TypedState`,
compares implementation state against the spec's state (`State.check`) after each step.
A step or state mismatch surfaces as an `AssertionError` naming the trace, step, action
and nondet picks (see `Runner.kt`).

`@QuintTest` needs `DriverConfig.nondetPath`, because `quint test` does not write the
`mbt::*` variables (`docs/decisions/quint-test-needs-nondet-path.md`).

## Conventions & Patterns

See `CLAUDE.md` for the Quint-to-Kotlin type mapping table and `docs/decisions/` for
standing project decisions (license, platform support, ITF collection/Option/BigInt
mapping, the `@QuintTest` nondet path requirement).
