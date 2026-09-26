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
