package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

// Set by the quintkonnect Gradle plugin's shrinkQuintTraces task (QuintKonnectPlugin.kt, qk-a8ay):
// makes ReplayRunner.traceReplays rerun quint for the first failing trace with the same seed and
// a smaller --max-steps, reporting the shortest failing trace it finds instead.
internal const val SHRINK_PROPERTY: String = "quintkonnect.shrink"

internal fun shrinkEnabled(value: String? = System.getProperty(SHRINK_PROPERTY)): Boolean = value == "true"

// [delegate]'s command with its `--max-steps` (from the config itself or `-Pquint.maxSteps`,
// RunConfig.toCommand) replaced by [maxSteps], so the shrink search isn't overridden by either.
internal class MaxStepsConfig(
    private val delegate: GeneratorConfig,
    val maxSteps: Int,
) : GeneratorConfig by delegate {
    override fun toCommand(tmpDir: Path): List<String> {
        val command = delegate.toCommand(tmpDir).toMutableList()
        val index = command.indexOf("--max-steps")
        if (index >= 0) command.subList(index, index + 2).clear()
        return command + listOf("--max-steps", maxSteps.toString())
    }
}
