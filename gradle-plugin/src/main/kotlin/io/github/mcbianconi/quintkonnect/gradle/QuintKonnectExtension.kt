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
}
