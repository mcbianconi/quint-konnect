# Gradle setup

quint-konnect isn't on Maven Central yet (tracked by quint-konnect bead qk-j02b). Every
coordinate below uses `<VERSION>` as a placeholder — ask the user for the version they're using,
or check whether they're consuming it via `includeBuild`/`publishToMavenLocal` from a local
checkout instead.

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
    id("io.github.mcbianconi.quint-konnect") version "<VERSION>"
    kotlin("plugin.serialization") version "2.4.20"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Only if a @QuintAction is `suspend`:
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}
```

Applying the plugin to a Kotlin JVM module:
- applies `com.google.devtools.ksp` and adds `kspTest("io.github.mcbianconi:quint-konnect-ksp")`
  and `testImplementation("io.github.mcbianconi:quint-konnect-core")` at the plugin's own
  version (`quint-konnect-core` brings in `quint-konnect-annotations` and
  `kotlinx-serialization-json` transitively — don't declare either yourself);
- adds `build/generated/ksp/test/kotlin` to the `test` source set (this is where the generated
  dispatcher and JUnit 5 test class land — don't create files there by hand);
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

## Without the plugin (manual fallback)

Use this when the plugin can't be applied yet (e.g. `pluginManagement` in this project can't add
`mavenCentral()`, or the project needs the KSP/dependency wiring done by hand for another reason).

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
    id("com.google.devtools.ksp") version "2.3.12"
}

dependencies {
    kspTest("io.github.mcbianconi:quint-konnect-ksp:<VERSION>")
    testImplementation("io.github.mcbianconi:quint-konnect-core:<VERSION>")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
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
    // stderr by default.
    testLogging {
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
