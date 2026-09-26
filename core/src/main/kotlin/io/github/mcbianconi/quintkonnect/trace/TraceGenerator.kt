package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.display
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
            val process = ProcessBuilder(command).start()

            var stdout = ""
            var stderr = ""
            val stdoutReader = thread(isDaemon = true) { stdout = process.inputStream.bufferedReader().readText() }
            val stderrReader = thread(isDaemon = true) { stderr = process.errorStream.bufferedReader().readText() }

            if (!process.waitFor(config.timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                error("Quint did not finish within ${config.timeout}: ${command.joinToString(" ")}")
            }
            stdoutReader.join()
            stderrReader.join()

            if (process.exitValue() != 0) {
                if (config.invariants.isNotEmpty() && stderr.contains("Invariant violated")) {
                    val violated = violatedInvariants(config.invariants, stdout)
                    error(invariantViolationMessage(config.seed, violated, tmpDir))
                }
                error("Quint returned non-zero exit code.\n$stderr")
            }

            return readTraces(tmpDir)
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    // quint prints "error: Invariant violated" to stderr regardless of --verbosity, so that text is
    // the reliable signal that this non-zero exit is an invariant violation, not some other CLI
    // failure. At verbosity >= 1 (RunConfig sets this whenever invariants are configured) it also
    // prints one "❌ <name>" line per violated invariant on stdout, but only when 2+ invariants were
    // checked together; with a single configured invariant, the violation itself already identifies
    // it unambiguously.
    private val violatedInvariantMarker = Regex("""❌\s*(\S+)""")

    private fun violatedInvariants(invariants: List<String>, stdout: String): List<String> {
        if (invariants.size == 1) return invariants
        val reported = violatedInvariantMarker.findAll(stdout).map { it.groupValues[1] }.toList()
        return reported.ifEmpty { invariants }
    }

    private fun invariantViolationMessage(seed: String, violated: List<String>, tmpDir: Path): String {
        val traceDump = readTraces(tmpDir).firstOrNull()
            ?.let { "\nViolating trace:\n${renderTrace(it)}" }
            ?: ""
        return "Quint invariant violated: ${violated.joinToString(", ")} (seed $seed)$traceDump"
    }

    private fun renderTrace(trace: ItfTrace): String =
        trace.states.mapIndexed { idx, state ->
            "[$idx] " + state.value.entries.joinToString(", ") { (k, v) -> "$k: ${v.display()}" }
        }.joinToString("\n")

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
