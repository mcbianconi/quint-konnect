package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val DEFAULT_TRACES = 100

interface GeneratorConfig {
    val seed: String
    val nTraces: Int
    val timeout: Duration get() = 10.minutes
    fun toCommand(tmpDir: Path): List<String>
}
