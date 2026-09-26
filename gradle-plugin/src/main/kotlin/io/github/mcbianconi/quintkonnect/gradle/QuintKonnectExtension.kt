package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.provider.Property

// CI's pin (.github/workflows/ci.yml), also the default `checkQuint` compares against.
public const val DEFAULT_QUINT_VERSION: String = "0.32.0"

public abstract class QuintKonnectExtension {
    public abstract val quintVersion: Property<String>
}
