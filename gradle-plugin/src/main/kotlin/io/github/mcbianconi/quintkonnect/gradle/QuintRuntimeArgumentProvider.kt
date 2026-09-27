package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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
    @get:Input @get:Optional val replay: Provider<String>,
    // Fingerprints the replay file/directory's content: `replay` above is only the path, so a Test
    // task would otherwise stay UP-TO-DATE after the saved trace it points at changes.
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE) val replayFiles: FileCollection,
    @get:Input @get:Optional val parallelism: Provider<Int>,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = buildList {
        if (downloadQuint.get()) {
            add("-D$QUINT_EXECUTABLE_SYSTEM_PROPERTY=${quintExecutablePath.get()}")
        }
        maxSamples.orNull?.let { add("-D$MAX_SAMPLES_SYSTEM_PROPERTY=$it") }
        maxSteps.orNull?.let { add("-D$MAX_STEPS_SYSTEM_PROPERTY=$it") }
        seed.orNull?.let { add("-D$SEED_SYSTEM_PROPERTY=$it") }
        verbose.orNull?.let { add("-D$VERBOSE_SYSTEM_PROPERTY=$it") }
        replay.orNull?.let { add("-D$REPLAY_SYSTEM_PROPERTY=$it") }
        parallelism.orNull?.let { add("-D$PARALLELISM_SYSTEM_PROPERTY=$it") }
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

// Mirrors REPLAY_PROPERTY (core/.../trace/TraceSource.kt): set from the `-Pquint.replay` Gradle
// property below, resolved to an absolute path against the project directory. When set, Test
// tasks skip checkQuint/downloadQuint (QuintKonnectPlugin.kt): replaying a saved trace needs no
// quint installation.
internal const val REPLAY_SYSTEM_PROPERTY: String = "quintkonnect.replay"
internal const val REPLAY_GRADLE_PROPERTY: String = "quint.replay"

// Mirrors PARALLELISM_PROPERTY (core/.../ReplayRunner.kt): set from the `-Pquint.parallelism`
// Gradle property below. Only affects ReplayRunner.runTest's batch path; per-trace dynamic tests
// (ReplayRunner.traceReplays) parallelize through JUnit's own dynamic test execution instead.
internal const val PARALLELISM_SYSTEM_PROPERTY: String = "quintkonnect.parallelism"
internal const val PARALLELISM_GRADLE_PROPERTY: String = "quint.parallelism"

// Mirrors TRACES_DIR_PROPERTY (core/.../trace/TraceSource.kt): generateQuintTraces' output
// directory (GenerateQuintTracesTask.kt, qk-adm2), set on every Test task unless `-Pquint.replay`
// is also set (replaying a saved trace needs no generated-traces lookup either, mirroring
// QuintKonnectPlugin.kt's other `replayOverride == null` guards).
internal const val TRACES_DIR_SYSTEM_PROPERTY: String = "quintkonnect.tracesDir"

// A separate CommandLineArgumentProvider from the one above: [tracesDirFiles] fingerprints
// generateQuintTraces' output directory content as a Test task input (so a Test task reruns when
// the traces it would replay change), while [tracesDirPath] itself stays @Internal (a machine
// build-dir path); referencing generateQuintTraces' own output property here is also what makes a
// Test task depend on it, without a separate explicit dependsOn.
internal class TracesDirArgumentProvider(
    @get:Internal val tracesDirPath: Provider<String>,
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) val tracesDirFiles: FileCollection,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = listOf("-D$TRACES_DIR_SYSTEM_PROPERTY=${tracesDirPath.get()}")
}
