package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.quintkonnect.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.itf.parseTrace
import java.nio.file.Files
import java.nio.file.Path

object TraceGenerator {

    fun generate(config: GeneratorConfig): List<ItfTrace> {
        val tmpDir = Files.createTempDirectory("quint-konnect-")
        try {
            val command = config.toCommand(tmpDir)
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val stderr = process.errorStream.bufferedReader().readText()
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
