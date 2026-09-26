package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public const val DEFAULT_TRACES: Int = 100

public interface GeneratorConfig {
    public val seed: String
    public val nTraces: Int
    public val timeout: Duration get() = 10.minutes
    public fun toCommand(tmpDir: Path): List<String>
}

// Set by the quintkonnect Gradle plugin's Test tasks (gradle-plugin/.../QuintKonnectPlugin.kt) to
// the Gradle project directory, so a relative `spec` resolves the same way from an IDE run
// (working dir may not be the project dir) as from `gradle test` (working dir is the project dir).
internal const val PROJECT_DIR_PROPERTY: String = "quintkonnect.projectDir"

internal fun resolveSpec(spec: String, projectDir: String? = System.getProperty(PROJECT_DIR_PROPERTY)): String {
    if (projectDir == null) return spec
    val specPath = Path.of(spec)
    if (specPath.isAbsolute) return spec
    return Path.of(projectDir).resolve(specPath).toString()
}

// Set by the quintkonnect Gradle plugin's Test tasks and CheckQuintTask (gradle-plugin/.../
// QuintKonnectPlugin.kt, DownloadQuintTask.kt) to the downloaded quint executable's absolute path
// when `quintKonnect.downloadQuint` is enabled; otherwise unset, and "quint" is resolved from PATH
// as before.
internal const val QUINT_EXECUTABLE_PROPERTY: String = "quintkonnect.quintExecutable"

internal fun quintExecutable(quintExecutable: String? = System.getProperty(QUINT_EXECUTABLE_PROPERTY)): String =
    quintExecutable ?: "quint"

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.maxSamples`/`-Pquint.maxSteps` Gradle properties, for PR vs nightly CI profiles.
// Override > the annotation's maxSamples/maxSteps value (the RunConfig/TestConfig field below) >
// DEFAULT_TRACES/no `--max-steps` flag.
internal const val MAX_SAMPLES_PROPERTY: String = "quintkonnect.maxSamples"
internal const val MAX_STEPS_PROPERTY: String = "quintkonnect.maxSteps"

internal fun maxSamplesOverride(value: String? = System.getProperty(MAX_SAMPLES_PROPERTY)): Int? = value?.toIntOrNull()
internal fun maxStepsOverride(value: String? = System.getProperty(MAX_STEPS_PROPERTY)): Int? = value?.toIntOrNull()
