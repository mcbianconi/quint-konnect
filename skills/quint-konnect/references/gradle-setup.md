# Gradle setup

Current release is `0.1.0` on Maven Central under `io.github.mcbianconi`. Every coordinate below
uses that version; check https://repo1.maven.org/maven2/io/github/mcbianconi/ for a newer one.

Requires JDK 21, Kotlin 2.4.20+, and KSP 2.3.12+ (a KSP version pinned to a Kotlin version;
check https://github.com/google/ksp/releases for the pair matching the project's Kotlin version).

## With the Gradle plugin (preferred)

Add both repositories the plugin marker needs (it publishes to Maven Central only, not the
Gradle Plugin Portal):

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

On a module that already applies `org.jetbrains.kotlin.jvm`:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.20"
    id("io.github.mcbianconi.quint-konnect") version "0.1.0"
    kotlin("plugin.serialization") version "2.4.20"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Only if a @QuintAction is `suspend`:
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}
```

Any JUnit Jupiter 5.x or 6.x works: `gradle-plugin` adds no JUnit dependency itself, and
KSP-generated test classes only call `@TestFactory`/`DynamicTest.dynamicTest(String, Executable)`,
unchanged across that range. quint-konnect's own test suite ran on 5.14.4 before this project's
JUnit 6 upgrade and runs on 6.1.3 now — both verified, not just inferred from the stable API.

Applying the plugin to a Kotlin JVM module:
- applies `com.google.devtools.ksp` and adds `kspTest("io.github.mcbianconi:quint-konnect-ksp")`
  and `testImplementation("io.github.mcbianconi:quint-konnect-core")` at the plugin's own
  version (`quint-konnect-core` brings in `quint-konnect-annotations` and
  `kotlinx-serialization-json` transitively — don't declare either yourself);
- adds `build/generated/ksp/test/kotlin` to the `test` source set (this is where the generated
  dispatcher and JUnit Jupiter test class land — don't create files there by hand);
- configures every `Test` task with `useJUnitPlatform()` and a system property so a relative
  `spec` path (`@QuintRun`/`@QuintTest`) resolves against the Gradle project directory, not the
  test JVM's working directory. This only applies to test JVMs Gradle itself launches — an IDE
  test runner that bypasses Gradle still resolves a relative `spec` against its own working
  directory, which can differ. If a spec "not found" only happens from the IDE, that's why.
- registers a `checkQuint` task (every `Test` task depends on it) that fails the build if `quint`
  isn't on `PATH`, and warns (without failing) if its version doesn't match
  `quintKonnect.quintVersion` (default `"0.32.0"`).

```kotlin
quintKonnect {
    quintVersion.set("0.32.0") // default; only set this to pin a different version
}
```

The plugin configures every `Test` task's `testLogging` (`exceptionFormat = FULL`,
`showStandardStreams = true`, `events(FAILED)`), so a failure's trace/step/action/diff message
and the stderr reproduce line show in the console. Opt out with
`quintKonnect { configureTestLogging.set(false) }`; a `tasks.test { testLogging { ... } }` block
below `plugins { }` also overrides it.

## Testing an unreleased build (optional)

To test a change not yet released — after running `./gradlew publishToMavenLocal` in a
quint-konnect checkout — add `mavenLocal()` to **both** repository blocks in
`settings.gradle.kts` — the plugin marker resolves via `pluginManagement`, the library artifacts
via `dependencyResolutionManagement` — and set every coordinate's version to match that
checkout's `build-logic/src/main/kotlin/quintkonnect.kotlin-jvm.gradle.kts` version, not `0.1.0`:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
```

Do not also add a `repositories { ... }` block to `build.gradle.kts` on top of this: a
project-level `repositories` block makes Gradle ignore the ones declared in
`dependencyResolutionManagement` (it warns "The project declares repositories, effectively
ignoring the repositories you have declared in the settings"), which silently drops `mavenLocal()`
and produces a "Could not find io.github.mcbianconi:..." resolution failure that looks unrelated
to repositories at first glance. If the project already declares per-project repositories (i.e.
`dependencyResolutionManagement` isn't used), add `mavenLocal()` to that existing block instead.

## Without the plugin (manual fallback)

Use this when the plugin can't be applied yet (e.g. `pluginManagement` in this project can't add
`mavenCentral()`, or the project needs the KSP/dependency wiring done by hand for another reason).

```kotlin
// build.gradle.kts
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
    id("com.google.devtools.ksp") version "2.3.12"
}

dependencies {
    kspTest("io.github.mcbianconi:quint-konnect-ksp:0.1.0")
    testImplementation("io.github.mcbianconi:quint-konnect-core:0.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Only if a @QuintAction is `suspend`:
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
    // Without the plugin, wire this by hand so a relative `spec` resolves the same way from
    // `gradle test` and from an IDE run (matches PROJECT_DIR_PROPERTY in core's GeneratorConfig).
    systemProperty("quintkonnect.projectDir", project.projectDir.absolutePath)
    // Recommended: QUINT_VERBOSE/seed-reproduction output goes to stderr, and Gradle hides test
    // stderr by default. exceptionFormat = FULL is also needed, or the console only shows
    // "AssertionError at File.kt:N" with no trace/step/action/diff message.
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
```

There is no `checkQuint` task without the plugin: a missing or mismatched `quint` on `PATH`
surfaces as a trace-generation failure instead of a clear upfront error.

## Confirm `quint` is installed

```bash
quint --version
```

quint-konnect's CI and default `checkQuint` are pinned to **0.32.0**. Install/upgrade with:

```bash
npm i -g @informalsystems/quint@0.32.0
```
