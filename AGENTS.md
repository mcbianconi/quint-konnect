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
./gradlew :itf:test                  # Run ITF parsing/decoding unit tests
./gradlew :core:test                 # Run core unit tests (no quint CLI required)
./gradlew :ksp:build                 # Build KSP processor
./gradlew :ksp:test                  # Run KSP processor tests (kotlin-compile-testing + KSP2, no quint CLI required)
./gradlew :example:build             # Build example + run end-to-end test (requires quint in PATH)
./gradlew build                      # Build all modules
```

Run a single test with `--tests`, e.g. `./gradlew :core:test --tests TraceGeneratorTest`.
`QUINT_VERBOSE=1`/`2` and `QUINT_SEED=<hex>` control logging and reproducibility (see
README.md's Environment variables section).

`annotations`, `itf`, `core` and `ksp` (not `example`) build with Kotlin's explicit API
mode (`quintkonnect.library` convention plugin in `build-logic/`): every public
declaration needs an explicit `public`/`internal`/`private` modifier, and each module
keeps a reference ABI dump at `<module>/api/<module>.api`, checked by `checkKotlinAbi`
(runs as part of `check`/`build`). After a deliberate public API change in one of those
modules, regenerate its dump with `./gradlew :<module>:updateKotlinAbi` (or
`./gradlew updateKotlinAbi` for all of them) and commit the updated `.api` file.
Reified inline functions (`NondetPicks.decode`/`decodeOrNull`, `ItfValue.decode()`) don't
appear in the ABI dump, so `checkKotlinAbi` doesn't guard their signatures; review those
by hand.

## Releasing

`annotations`, `itf`, `core` and `ksp` publish to Maven Central under `io.github.mcbianconi`
(`quintkonnect.publish` convention plugin in `build-logic/`, the vanniktech
gradle-maven-publish-plugin); `example` is not published. `.github/workflows/release.yml`
triggers on pushing a tag matching `v*` and runs `./gradlew publishAndReleaseToMavenCentral`.

To release: bump `version` in
`build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`, commit, then push a tag
`v<that version>` (e.g. `v0.1.0`). The tag only triggers the workflow — it always publishes
whatever version is currently set in that file, so pushing a tag without bumping the
version re-uploads the existing version and Central rejects it as a duplicate. Keep the
tag name matching the version anyway, for a readable history.

To test locally without publishing anywhere remote: `./gradlew publishToMavenLocal`.
Signing is skipped unless `signingInMemoryKey` is set as a Gradle property, so this works
without keys; check `~/.m2/repository/io/github/mcbianconi/` for the result.

## Architecture Overview

Five modules, in dependency order:

- `annotations` — `@QuintRun`, `@QuintTest`, `@QuintAction` declarations only. No runtime
  dependency, so it stays on a driver's compile classpath without pulling in `core`.
- `itf` — ITF parsing and decoding (`ItfValue`, `ItfTrace`/`parseTrace`,
  `ItfValueSerializer`, `ItfValue.decode`). `ItfValue.decode` runs an internal kotlinx
  `Decoder` over the `ItfValue` tree.
- `core` — the runtime: `quint` CLI invocation and trace generation (`trace/`, behind the
  injectable `TraceSource`), step extraction (`Step.kt`), nondet pick decoding (`nondet/`),
  state comparison (`State.kt`), and the replay loop (`ReplayRunner`, observed by a
  `ReplayListener`; `listener/ConsoleReplayListener` is the default). `Runner` is the
  facade generated tests call, delegating to a fresh `ReplayRunner`.
- `ksp` — a KSP2 processor that reads `@QuintRun`/`@QuintTest`/`@QuintAction` on a driver
  class and generates a JUnit 5 test class plus a `generatedStep()` dispatcher
  (`ksp/generators/`). `Driver.step`'s default implementation (`core`) finds the generated
  dispatcher by class name, so a driver doesn't need to override `step` itself.
- `example` — end-to-end examples: TicTacToe, rock-paper-scissors, a buggy driver the
  tests expect to fail, a `@QuintTest` counter, and a fixture for escaped names.

Data flow: a Quint spec is run through the `quint` CLI (`quint run --mbt` or
`quint test`) to produce ITF trace files; the default `TraceSource` (`TraceGenerator`)
invokes the CLI and parses the output into `ItfTrace`/`ItfValue`; `ReplayRunner` replays
each trace step against the driver (generated `generatedStep()` dispatches to the right
`@QuintAction` method, decoding nondet picks into method parameters) and, when the driver
provides a `TypedState`, compares implementation state against the spec's state
(`State.check`) after each step. A step or state mismatch surfaces as an `AssertionError`
naming the trace, step, action and nondet picks (see `ReplayRunner.kt`), and every
`ReplayListener` (the console one included) is notified of the failure before it's thrown.
Construct a `ReplayRunner` directly to plug in a custom `TraceSource` or `ReplayListener`;
`Runner.runTest` stays the entry point generated code calls, delegating to a default
`ReplayRunner`.

`@QuintTest` needs `DriverConfig.nondetPath`, because `quint test` does not write the
`mbt::*` variables (`docs/decisions/quint-test-needs-nondet-path.md`).

## Conventions & Patterns

See `CLAUDE.md` for the Quint-to-Kotlin type mapping table and `docs/decisions/` for
standing project decisions (license, platform support, ITF collection/Option/BigInt
mapping, the `@QuintTest` nondet path requirement).
