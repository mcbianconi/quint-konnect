---
type: is
id: is-01m3j7vnc9vr73kchj9xd35bcc
title: Turn example/ into a standalone Gradle build that applies the quint-konnect plugin
kind: task
status: in_progress
priority: 3
version: 3
delegate: claude-code@vm
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3j7vnm12cr9nymxdcvkbjqn
parent_id: is-01m3j7vmxf3e87tr20a324y2s9
hold: null
hold_until: null
created_at: 2026-09-27T20:10:47.048Z
updated_at: 2026-09-27T20:33:36.671Z
started_at: 2026-09-27T20:33:36.671Z
---
Goal: `example/build.gradle.kts` looks like README.md's setup section.

- Remove `:example` from root `settings.gradle.kts` `include(...)` (otherwise the composite
  below is a cycle).
- Add `example/settings.gradle.kts` with `pluginManagement { includeBuild("..") }` and
  `includeBuild("..")`, with a comment linking
  https://docs.gradle.org/current/userguide/composite_builds.html explaining that this stands in
  for the Maven Central coordinates a user would use. Add `example/gradle` wrapper or document
  running it as `./gradlew -p example build`.
- Build file: `kotlin("jvm")`, `kotlin("plugin.serialization")`,
  `id("io.github.mcbianconi.quint-konnect")`, `quintKonnect { readSpecIr.set(true) }`, JUnit and
  `kotlinx-coroutines-core` (suspending example). Delete the `quintIr`/`quintIr_*` Exec tasks,
  `ksp { arg(...) }`, `kotlin.srcDir(...)`, the `kspTestKotlin` `inputs.dir` hack,
  `systemProperty("quintkonnect.projectDir", ...)`, and the `quintkonnect.*` convention plugins.
- Versions: reuse the root catalog via
  `dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } } }`
  so Kotlin stays compatible with the KSP version the plugin applies.
- Trap: published artifactIds (`quint-konnect-ksp`, `quint-konnect-core`, ...) don't match the
  project names (`:ksp`, `:core`), so default composite substitution may not apply and Gradle
  would quietly pull `0.1.0` from Maven Central. Declare `dependencySubstitution` in the
  `includeBuild("..")` block if needed.
- Code that existed only because the plugin was absent goes: e.g. the "example wires KSP by
  hand" comments in drivers/tests. Where a test in `example/` can now use a plugin feature
  (`-Pquint.replay`, `shrinkQuintTraces`, `@QuintRun(invariants = [...])`), show it in README of
  the example or a short test, not via internal APIs.
- CI: `.github/workflows/ci.yml` runs only `./gradlew build` (line 37); add a step for the
  example build.
- Update references: `grep -rn 'example/\|:example' README.md AGENTS.md skills/ docs/ ksp/ core/ gradle-plugin/`
  (AGENTS.md's `./gradlew :example:build` line, README links, skill references per the CLAUDE.md
  sync rule).

Acceptance:
- `./gradlew -p example dependencyInsight --configuration kspTest --dependency quint-konnect-ksp`
  (and `testCompileClasspath` / `quint-konnect-core`) resolves to `project :ksp` / `:core` of the
  included build, not a Maven Central version.
- `./gradlew -p example build` passes with generated tests running; `./gradlew build` at root passes.
- `example/build.gradle.kts` contains nothing a README reader wouldn't write.
