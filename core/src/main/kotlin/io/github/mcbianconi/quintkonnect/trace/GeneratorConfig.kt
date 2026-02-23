package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

const val DEFAULT_TRACES = 100

interface GeneratorConfig {
    val seed: String
    val nTraces: Int
    fun toCommand(tmpDir: Path): List<String>
}
