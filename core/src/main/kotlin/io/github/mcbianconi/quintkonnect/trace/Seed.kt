package io.github.mcbianconi.quintkonnect.trace

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.seed` Gradle property, for PR vs nightly CI profiles.
//
// Precedence is override > QUINT_SEED > random here, not override > annotation value > default:
// `@QuintRun(seed = "...")`/`@QuintTest(seed = "...")` are SOURCE-retention annotations, and the
// KSP generator (ksp/.../QuintRunTestGenerator.kt, QuintTestTestGenerator.kt) bakes a non-blank
// annotation seed straight into the generated `RunConfig(seed = "...", ...)`/`TestConfig(...)`
// call, never calling this function at all in that case; RunConfig/TestConfig are public data
// classes (checkKotlinAbi-guarded), so intercepting an explicit annotation seed here would need
// either a breaking constructor change or a KSP change (ksp/ isn't this change's file).
internal const val SEED_PROPERTY: String = "quintkonnect.seed"

public fun genSeed(): String =
    System.getProperty(SEED_PROPERTY)
        ?: System.getenv("QUINT_SEED")
        ?: "0x%x".format((Math.random() * Int.MAX_VALUE).toLong())
