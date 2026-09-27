package io.github.mcbianconi.quintkonnect.gradle

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

// Mirrors IR_DIR_OPTION_NAME in ksp/.../ir/QuintIr.kt: the KSP processor option this task's
// outputDir is passed under (internal visibility is per-module, so this can't just reference
// ksp's constant). See docs/decisions/quint-ir-source.md for why `typecheck`, not `compile`.
internal const val QUINT_IR_DIR_OPTION: String = "quintkonnect.irDir"

@CacheableTask
public abstract class QuintIrTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val specs: ConfigurableFileCollection

    // Only used to compute each output file's path relative to it (matching a spec's own relative
    // `@QuintRun`/`@QuintTest` path); not a build input in its own right.
    @get:Internal
    public abstract val projectDirectory: Property<String>

    @get:Input
    public abstract val quintExecutable: Property<String>

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    init {
        group = "build"
        description = "Runs `quint typecheck` on every configured spec, for KSP to read action/nondet types from."
    }

    @TaskAction
    public fun run() {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        val root = File(projectDirectory.get())

        specs.forEach { spec ->
            val relativePath = spec.relativeTo(root).path
            val outFile = File(outDir, "$relativePath.json")
            outFile.parentFile.mkdirs()

            val stderr = ByteArrayOutputStream()
            val exitValue = execOperations.exec {
                it.commandLine(quintExecutable.get(), "typecheck", "--out", outFile.absolutePath, spec.absolutePath)
                it.errorOutput = stderr
                it.isIgnoreExitValue = true
            }.exitValue

            if (exitValue != 0) {
                val explanation = outFile.takeIf { it.isFile }?.let(::readErrors).orEmpty()
                outFile.delete()
                val stderrText = stderr.toString(Charsets.UTF_8).trim()
                throw GradleException(
                    buildString {
                        append("quint typecheck failed for $relativePath (exit $exitValue).")
                        if (explanation.isNotBlank()) append("\n").append(explanation)
                        if (stderrText.isNotBlank()) append("\n").append(stderrText)
                    },
                )
            }
        }
    }
}

// `quint typecheck --out <file>` suppresses console output entirely, even on failure: the only
// diagnostic is the written file's own top-level "errors" array (each with an "explanation").
private fun readErrors(file: File): String {
    val root = Json.parseToJsonElement(file.readText()).jsonObject
    val errors = root["errors"]?.jsonArray ?: return ""
    return errors.joinToString("\n") { error ->
        error.jsonObject["explanation"]?.jsonPrimitive?.contentOrNull ?: error.toString()
    }
}
