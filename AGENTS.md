# Project instructions

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

## Commands

This repository's build needs JDK 21 and `quint` 0.32.0 on `PATH`. CI pins `@informalsystems/quint@0.32.0` in `.github/workflows/ci.yml`. A consumer project can set `quintKonnect.downloadQuint` instead of installing `quint`. The plugin's functional test points that download at a local `file://` fixture.

`./gradlew build` builds the root modules. It does not build `example`. `example` is a separate build. Run `./gradlew -p example build`.

These tasks need `quint` on `PATH`.

- `./gradlew :gradle-plugin:functionalTest`. One test asserts the real `checkQuint` version warning.
- `./gradlew :integration-tests:test`
- `./gradlew -p example build`

`:core:test`, `:ksp:test`, `:itf:test`, and `:gradle-plugin:test` do not. `:ksp:test` uses kotlin-compile-testing. `:gradle-plugin:test` uses ProjectBuilder.

Run one test with `--tests`, for example `./gradlew :core:test --tests TraceGeneratorTest`.

`QUINT_VERBOSE` is `0`, `1`, or `2`. `QUINT_SEED` fixes the seed. `QUINT_COLOR` is `always` or `never`.

These Gradle properties override one test run without a recompile: `-Pquint.maxSamples`, `-Pquint.maxSteps`, `-Pquint.seed`, `-Pquint.verbose` (`0`, `1`, or `2`), and `-Pquint.replay=<path>`. Seed order is `-Pquint.seed`, then a non-empty annotation `seed`, then `QUINT_SEED`, then a random seed. The system-property names and the rest of the precedence are in README.md, under "Override a run from Gradle".

`-Pquint.parallelism` changes only `Runner.runTest`. Generated tests do not read it. They parallelize through JUnit. See README.md, "Run traces in parallel".

`-Pquint.replay` does not run `quint` at test time. When `readSpecIr` is true, `quintIr` still runs. See `docs/decisions/replay-needs-quint-with-read-spec-ir.md`.

`generateTraces` defaults to false. Quint then runs during the test, so `--tests` starts it only for the drivers that run. A `Test` task depends on `checkQuint` only when that task will run `quint`.

Format with Spotless, not a ktlint Gradle plugin.

```bash
./gradlew spotlessApply -PspotlessIdeHook=<absolute path>
./gradlew spotlessCheck
```

The first command formats one file. Plain `spotlessApply` rewrites every uncommitted Kotlin file in the checkout. Use the single-file form when other agents share the GitButler workspace.

`but commit` and `but land` do not run git hooks. `spotlessCheck` runs on `git push`, `but push`, and `but pr new`. A violation runs `spotlessApply` and fails the push. `/ship-it` runs `build` before it lands.

## Landmines

- `@QuintTest` traces have no `mbt::*` variables. Set `DriverConfig.nondetPath`. See `docs/decisions/quint-test-needs-nondet-path.md`.
- `annotations`, `itf`, `core`, `ksp`, and `gradle-plugin` keep an ABI dump at `<module>/api/<module>.api`. `checkKotlinAbi` runs as part of `check`. After a public API change, run `./gradlew :<module>:updateKotlinAbi` and commit the dump. `./gradlew updateKotlinAbi` updates every module. Reified inline functions are absent from the dump: `NondetPicks.decode`, `NondetPicks.decodeOrNull`, and `ItfValue.decode`. Review those signatures by hand. A new public class is an API change.
- This project does not target Windows. See `docs/decisions/no-windows-support.md`.
- The Gradle plugin publishes to Maven Central, not the Gradle Plugin Portal. See `docs/decisions/gradle-plugin-distribution.md`.
- The plugin adds no JUnit dependency. The generated adapter calls `@TestFactory` and `DynamicTest.dynamicTest` only.

## Releasing

`annotations`, `itf`, `core`, `ksp`, and `gradle-plugin` publish under `io.github.mcbianconi`. `example` and `integration-tests` do not.

`.github/workflows/release.yml` runs on a tag that matches `v*`. It publishes the `version` in `build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts`. The tag does not select the version. A tag that does not match that file republishes the current version, and Maven Central rejects the duplicate. Bump the version, then push a tag `v<version>` with the same number.

In that same commit, set `.claude-plugin/plugin.json` `version` to the library version. Update every remaining copy of the old version under `skills/quint-konnect/` and in README.md. Search those trees for the version you are replacing. See `docs/decisions/agent-skill-plugin.md`.

`./gradlew publishToMavenLocal` publishes locally and does not sign unless `signingInMemoryKey` is set. The files are under `~/.m2/repository/io/github/mcbianconi/`.

## Policy

The Quint-to-Kotlin type table is `skills/quint-konnect/references/types.md`. Do not copy it into another doc.

A change to `annotations`, `Driver`, `TypedState`, `DriverConfig`, the Gradle plugin, or the `QUINT_SEED`, `QUINT_VERBOSE`, `QUINT_COLOR`, or `-Pquint.*` properties updates `skills/quint-konnect/` in the same change.

Add a file under `docs/decisions/` only when the decision is not already in the code, the tests, the KDoc, or another doc.
