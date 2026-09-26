package io.github.mcbianconi.quintkonnect.trace

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.seed` Gradle property, for PR vs nightly CI profiles.
internal const val SEED_PROPERTY: String = "quintkonnect.seed"

internal fun resolveSeed(
    annotationSeed: String,
    override: String? = System.getProperty(SEED_PROPERTY),
    envSeed: String? = System.getenv("QUINT_SEED"),
): String =
    override
        ?: annotationSeed.takeIf { it.isNotBlank() }
        ?: envSeed
        ?: "0x%x".format((Math.random() * Int.MAX_VALUE).toLong())

public fun genSeed(): String = resolveSeed(annotationSeed = "")

/**
 * Resolves the seed for a `@QuintRun`/`@QuintTest`-generated test: the `-Pquint.seed` override
 * (system property [SEED_PROPERTY]) if set, else [annotationSeed] if non-blank, else `QUINT_SEED`,
 * else a random seed.
 */
public fun genSeed(annotationSeed: String): String = resolveSeed(annotationSeed)
