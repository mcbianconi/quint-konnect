package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

// Test.systemProperty()/systemProperties() aren't tracked as task inputs for up-to-date checking
// (Test/AbstractTestTask don't annotate their systemProperties getter @Input), so this is a
// CommandLineArgumentProvider instead: it lets each value be marked @Input (reruns Test tasks when
// it changes) or @Internal (machine-specific, e.g. a download path, excluded from that check)
// individually.
internal class QuintRuntimeArgumentProvider(
    @get:Input val downloadQuint: Provider<Boolean>,
    @get:Internal val quintExecutablePath: Provider<String>,
    @get:Input @get:Optional val maxSamples: Provider<Int>,
    @get:Input @get:Optional val maxSteps: Provider<Int>,
    @get:Input @get:Optional val seed: Provider<String>,
    @get:Input @get:Optional val verbose: Provider<Int>,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = buildList {
        if (downloadQuint.get()) {
            add("-D$QUINT_EXECUTABLE_SYSTEM_PROPERTY=${quintExecutablePath.get()}")
        }
        maxSamples.orNull?.let { add("-D$MAX_SAMPLES_SYSTEM_PROPERTY=$it") }
        maxSteps.orNull?.let { add("-D$MAX_STEPS_SYSTEM_PROPERTY=$it") }
        seed.orNull?.let { add("-D$SEED_SYSTEM_PROPERTY=$it") }
        verbose.orNull?.let { add("-D$VERBOSE_SYSTEM_PROPERTY=$it") }
    }
}

// Mirrors QUINT_EXECUTABLE_PROPERTY in core/.../trace/GeneratorConfig.kt: RunConfig/TestConfig
// resolve this system property instead of plain "quint" when a Test task sets it (internal
// visibility is per-module, so this can't just reference core's constant).
internal const val QUINT_EXECUTABLE_SYSTEM_PROPERTY: String = "quintkonnect.quintExecutable"

// Mirror MAX_SAMPLES_PROPERTY/MAX_STEPS_PROPERTY (core/.../trace/GeneratorConfig.kt) and
// SEED_PROPERTY (core/.../trace/Seed.kt) and VERBOSE_PROPERTY
// (core/.../listener/ConsoleReplayListener.kt): set from the `-Pquint.maxSamples`/
// `-Pquint.maxSteps`/`-Pquint.seed`/`-Pquint.verbose` Gradle properties below when present.
internal const val MAX_SAMPLES_SYSTEM_PROPERTY: String = "quintkonnect.maxSamples"
internal const val MAX_STEPS_SYSTEM_PROPERTY: String = "quintkonnect.maxSteps"
internal const val SEED_SYSTEM_PROPERTY: String = "quintkonnect.seed"
internal const val VERBOSE_SYSTEM_PROPERTY: String = "quintkonnect.verbose"

internal const val MAX_SAMPLES_GRADLE_PROPERTY: String = "quint.maxSamples"
internal const val MAX_STEPS_GRADLE_PROPERTY: String = "quint.maxSteps"
internal const val SEED_GRADLE_PROPERTY: String = "quint.seed"
internal const val VERBOSE_GRADLE_PROPERTY: String = "quint.verbose"
