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
