package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.parseTrace
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal object TraceGenerator : TraceSource {

    override fun generate(config: GeneratorConfig): List<ItfTrace> {
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

    private val sequenceNumberRegex = Regex("\\d+")

    private fun readTraces(tmpDir: Path): List<ItfTrace> =
        tmpDir.toFile()
            .listFiles()
            ?.sortedWith(compareBy({ sequenceNumber(it.name) }, { it.name }))
            ?.map { file -> parseTrace(file.readText()) }
            ?: emptyList()

    // quint's --out-itf {seq} placeholder isn't zero-padded
    private fun sequenceNumber(fileName: String): Long? =
        sequenceNumberRegex.find(fileName)?.value?.toLongOrNull()
}
