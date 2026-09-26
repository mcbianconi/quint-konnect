package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.parseTrace
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object TraceGenerator {

    fun generate(config: GeneratorConfig): List<ItfTrace> {
        val tmpDir = Files.createTempDirectory("quint-konnect-")
        try {
            val command = config.toCommand(tmpDir)
            val process = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()

            var stderr = ""
            val stderrReader = thread(isDaemon = true) { stderr = process.errorStream.bufferedReader().readText() }

            if (!process.waitFor(config.timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                error("Quint did not finish within ${config.timeout}: ${command.joinToString(" ")}")
            }
            stderrReader.join()

            if (process.exitValue() != 0) {
                error("Quint returned non-zero exit code.\n$stderr")
            }

            return readTraces(tmpDir)
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    private fun readTraces(tmpDir: Path): List<ItfTrace> =
        tmpDir.toFile()
            .listFiles()
            ?.sortedBy { it.name }
            ?.map { file -> parseTrace(file.readText()) }
            ?: emptyList()
}
