package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.provider.Property

// CI's pin (.github/workflows/ci.yml), also the default `checkQuint` compares against.
public const val DEFAULT_QUINT_VERSION: String = "0.32.0"

public abstract class QuintKonnectExtension {
    public abstract val quintVersion: Property<String>

    // Opt-in: false keeps resolving "quint" from PATH (existing setups keep working unchanged).
    // true downloads quintVersion into the Gradle user home cache (DownloadQuintTask.kt) and
    // points Test tasks and checkQuint at it instead, so a fresh clone needs no Node/quint install.
    public abstract val downloadQuint: Property<Boolean>

    // Opt-out: true (the default) configures every Test task's testLogging with
    // exceptionFormat=FULL, showStandardStreams=true and events(FAILED), so a quint-konnect
    // failure's trace/step/action/diff message and its "Reproduce this error with" stderr line
    // both show up in the console instead of only in build/test-results/test/*.xml. Set to false
    // to configure testLogging yourself; a project's own testLogging configuration always wins
    // over the plugin's regardless of this flag, as long as it's applied after the plugin (e.g. in
    // the same build.gradle.kts, below the `plugins {}` block).
    public abstract val configureTestLogging: Property<Boolean>
}
