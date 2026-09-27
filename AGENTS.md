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
comment on that line in `.github/workflows/ci.yml`). A consumer project can instead set
`quintKonnect { downloadQuint.set(true) }` to have the Gradle plugin download the pinned version
itself (README.md's "Downloading quint instead of installing it" section); `gradle-plugin`'s own
`functionalTest` exercises that path against a local `file://` fixture, not a real download.

This repo's own tests run on JUnit 6.1.3 (`gradle/libs.versions.toml`'s `junit` version). The
`gradle-plugin` doesn't add a JUnit dependency itself (see its section below) — a consumer picks
their own JUnit Jupiter version. KSP-generated test classes only call `@TestFactory` and
`DynamicTest.dynamicTest(String, Executable)`, unchanged from JUnit Jupiter 5.0 through 6.x, so a
consumer can stay on JUnit 5 or move to JUnit 6 independently of this project's own version; see
README.md's "Supported JUnit versions" section.

```bash
./gradlew :annotations:build         # Build annotation declarations
./gradlew :itf:test                  # Run ITF parsing/decoding unit tests
./gradlew :core:test                 # Run core unit tests (no quint CLI required)
./gradlew :ksp:build                 # Build KSP processor
./gradlew :ksp:test                  # Run KSP processor tests (kotlin-compile-testing + KSP2, no quint CLI required)
./gradlew :gradle-plugin:test         # Run the Gradle plugin's unit tests (ProjectBuilder, no quint CLI required)
./gradlew :gradle-plugin:functionalTest # Run its TestKit functional tests (requires quint in PATH: one test asserts checkQuint's real-mismatch warning)
./gradlew :example:build             # Build example + run end-to-end test (requires quint in PATH)
./gradlew :integration-tests:test    # Run regression tests against real quint (requires quint in PATH)
./gradlew build                      # Build all modules
```

Run a single test with `--tests`, e.g. `./gradlew :core:test --tests TraceGeneratorTest`.
`QUINT_VERBOSE=1`/`2`, `QUINT_SEED=<hex>` and `QUINT_COLOR=always|never` control logging,
reproducibility and colours (see README.md's Environment variables section).

A project applying the Gradle plugin can also override `@QuintRun`/`@QuintTest`'s `maxSamples`,
`maxSteps` and `seed`, plus the console listener's verbosity, per invocation (no recompile) with
`-Pquint.maxSamples=<int>`, `-Pquint.maxSteps=<int>`, `-Pquint.seed=<hex>` and
`-Pquint.verbose=0|1|2` — see README.md's "Runtime overrides for PR vs nightly CI profiles"
section for the full precedence and the `quintkonnect.*` system properties they map to.
`-Pquint.replay=<path>` replays a saved `.itf.json` trace instead of generating new ones (no
`quint` installation needed; see README.md's "Replaying a saved trace"), and
`-Pquint.parallelism=<int>` runs `Runner.runTest`'s traces on a thread pool of that size (see
README.md's "Running traces in parallel"; generated tests parallelize through JUnit's own dynamic
test execution instead).

`annotations`, `itf`, `core`, `ksp` and `gradle-plugin` (not `example` or `integration-tests`)
build with Kotlin's explicit API mode (`quintkonnect.library` convention plugin in `build-logic/`): every public
declaration needs an explicit `public`/`internal`/`private` modifier, and each module
keeps a reference ABI dump at `<module>/api/<module>.api`, checked by `checkKotlinAbi`
(runs as part of `check`/`build`). After a deliberate public API change in one of those
modules, regenerate its dump with `./gradlew :<module>:updateKotlinAbi` (or
`./gradlew updateKotlinAbi` for all of them) and commit the updated `.api` file.
Reified inline functions (`NondetPicks.decode`/`decodeOrNull`, `ItfValue.decode()`) don't
appear in the ABI dump, so `checkKotlinAbi` doesn't guard their signatures; review those
by hand.

## Releasing

`annotations`, `itf`, `core`, `ksp` and `gradle-plugin` publish to Maven Central under
`io.github.mcbianconi` (`quintkonnect.publish` convention plugin in `build-logic/`, the
vanniktech gradle-maven-publish-plugin); `example` and `integration-tests` are not
published. `gradle-plugin` also publishes its plugin marker artifact (`java-gradle-plugin`),
not to the Gradle Plugin Portal (`docs/decisions/gradle-plugin-distribution.md`).
`.github/workflows/release.yml`
triggers on pushing a tag matching `v*`, runs `./gradlew publishAndReleaseToMavenCentral`,
generates categorized release notes from conventional commits via `git-cliff` (`cliff.toml`),
and creates the GitHub Release with `gh release create --notes-file`.

To release: bump `version` in
`build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`, commit, then push a tag
`v<that version>` (e.g. `v0.1.0`). The tag only triggers the workflow — it always publishes
whatever version is currently set in that file, so pushing a tag without bumping the
version re-uploads the existing version and Central rejects it as a duplicate. Keep the
tag name matching the version anyway, for a readable history.

To test locally without publishing anywhere remote: `./gradlew publishToMavenLocal`.
Signing is skipped unless `signingInMemoryKey` is set as a Gradle property, so this works
without keys; check `~/.m2/repository/io/github/mcbianconi/` for the result.

`.claude-plugin/plugin.json`'s `version` must equal the library version and is bumped in the
same commit (`docs/decisions/agent-skill-plugin.md`). Since `skills/quint-konnect/` and
README.md now spell out `0.1.0` as literal coordinates rather than a `<VERSION>` placeholder,
a version bump must also update every `0.1.0` under `skills/quint-konnect/` and in README.md
(`grep -rn '0\.1\.0' skills/quint-konnect README.md` to find them), or the skill goes stale
against the new release.

## Architecture Overview

Seven modules, in dependency order:

- `annotations` — `@QuintRun`, `@QuintTest`, `@QuintAction` declarations only. No runtime
  dependency, so it stays on a driver's compile classpath without pulling in `core`.
- `itf` — ITF parsing and decoding (`ItfValue`, `ItfTrace`/`parseTrace`,
  `ItfValueSerializer`, `ItfValue.decode`). `ItfValue.decode` runs an internal kotlinx
  `Decoder` over the `ItfValue` tree.
- `core` — the runtime: `quint` CLI invocation and trace generation (`trace/`, behind the
  injectable `TraceSource`), step extraction (`Step.kt`), nondet pick decoding (`nondet/`),
  state comparison (`State.kt`), and the replay loop (`ReplayRunner`, observed by a
  `ReplayListener`; `listener/ConsoleReplayListener` is the default). Generated tests call
  `ReplayRunner.traceReplays` to get one `TraceReplay` per trace; `Runner.runTest` is kept for
  binary compatibility.
- `ksp` — a KSP2 processor that reads `@QuintRun`/`@QuintTest`/`@QuintAction` on a driver
  class and generates a JUnit Jupiter test class plus a `generatedStep()` dispatcher
  (`ksp/generators/`). `Driver.step`'s default implementation (`core`) finds the generated
  dispatcher by class name, so a driver doesn't need to override `step` itself. With spec IR
  (`quintkonnect.irDir`), it also generates `object <Module>Spec` of `@Serializable` spec types
  (`generators/SpecTypesGenerator.kt`) and defers such a driver one round so signatures that
  reference them resolve.
- `gradle-plugin` — a Gradle plugin (`io.github.mcbianconi.quint-konnect`,
  `QuintKonnectPlugin`) that, on a Kotlin JVM project, applies KSP, adds the `kspTest`/
  `testImplementation` dependencies on `ksp`/`core`, wires the KSP-generated test source
  directory, configures `Test` tasks (`useJUnitPlatform()`, the project-dir system property
  `core`'s `RunConfig`/`TestConfig` resolve a relative `spec` against), and registers
  `checkQuint` (every `Test` task depends on it) to fail on a missing `quint` and warn on a
  version mismatch against `quintKonnect.quintVersion`. It also configures `testLogging`
  (opt out with `quintKonnect.configureTestLogging`) and registers `quintIr`, which runs
  `quint typecheck --out` on `quintKonnect.quintIrSpecs` and, when `quintKonnect.readSpecIr`
  is true, passes the IR directory to KSP as the `quintkonnect.irDir` option
  (docs/decisions/quint-ir-source.md). It registers `generateQuintTraces`, which runs quint once
  per driver into `build/quint-konnect/traces/` for Test tasks to replay
  (docs/decisions/generate-quint-traces-task.md), and `shrinkQuintTraces`, a Test task over
  `test`'s classes that runs quint itself with `quintkonnect.shrink` set so `ReplayRunner`
  reports the shortest failing trace (core's `trace/Shrink.kt`). Tested with `ProjectBuilder` (`test`)
  and Gradle TestKit (`functionalTest`, applies the plugin to a fixture project via
  `withPluginClasspath()`).
- `example` — end-to-end examples: TicTacToe (state types generated from the spec's IR),
  rock-paper-scissors, a buggy driver the tests expect to fail, and a `@QuintTest` counter. Wires KSP and quint-konnect dependencies by hand (not through
  `gradle-plugin`, to avoid a `publishToMavenLocal` dependency in tests) but sets the same
  project-dir system property and mirrors `quintIr` with one `quint typecheck` task per spec.
- `integration-tests` — unpublished regression tests against real quint that go through
  `core`'s internals rather than what a user would write: exact shrink output, a saved-trace
  replay via `ItfFileTraceSource`, an invariant violation through `Runner.runTest`, and the
  escaped-names fixture (qk-gu38). Wired like `example` (KSP and `core` as project dependencies,
  hand-rolled `quintIr`).

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
generated test classes expose `@TestFactory fun traces(): List<DynamicTest>` built from
`ReplayRunner.traceReplays`, and `Runner.runTest` stays for binary compatibility.

`@QuintTest` needs `DriverConfig.nondetPath`, because `quint test` does not write the
`mbt::*` variables (`docs/decisions/quint-test-needs-nondet-path.md`).

## Conventions & Patterns

See `skills/quint-konnect/references/types.md` for the Quint-to-Kotlin type mapping table and
`docs/decisions/` for standing project decisions (license, platform support, ITF
collection/Option/BigInt mapping, the `@QuintTest` nondet path requirement, `gradle-plugin`'s
Maven-Central-only distribution).

## Keeping the agent skill in sync

A change to `annotations`, `Driver`, `TypedState`, `DriverConfig`, the Gradle plugin, or the
`QUINT_SEED`/`QUINT_VERBOSE`/`QUINT_COLOR` env vars or Gradle properties must update
`skills/quint-konnect/` in the same change.
