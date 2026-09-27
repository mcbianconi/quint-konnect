package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.parseTrace
import java.nio.file.Files
import java.nio.file.Path

private val trailingDigitsRegex = Regex("\\d+")

// Matches the last run of digits in the file name, not the first: a directory of saved failures
// (FailureTraceWriter.kt) names files "<testName>-trace<N>.itf.json", and testName can itself
// contain digits (e.g. "Foo2Test"), which would sort wrong under a first-match rule.
private fun sequenceNumber(fileName: String): Long? =
    trailingDigitsRegex.findAll(fileName).lastOrNull()?.value?.toLongOrNull()

/**
 * Replays trace(s) saved as ITF JSON instead of invoking `quint`: [path] a single `.itf.json` file
 * replays as one trace, a directory replays every regular file directly inside it (not recursive),
 * read in numeric order by the last run of digits in each file name (matching the order
 * [TraceGenerator] and `FailureTraceWriter` name files in).
 *
 * Construct this directly to replay a specific file or directory, or set the `quintkonnect.replay`
 * system property (the quintkonnect Gradle plugin's `-Pquint.replay` override, resolved against
 * the project directory) to use it as [io.github.mcbianconi.quintkonnect.ReplayRunner]'s default
 * trace source without constructing one by hand.
 */
public class ItfFileTraceSource(internal val path: Path) : TraceSource {

    override fun generate(config: GeneratorConfig): List<ItfTrace> {
        val absolutePath = path.toAbsolutePath()
        check(Files.exists(path)) { "No trace file or directory found at $absolutePath" }

        val files = if (Files.isDirectory(path)) {
            path.toFile().listFiles { f -> f.isFile }
                ?.sortedWith(compareBy({ sequenceNumber(it.name) }, { it.name }))
                ?: emptyList()
        } else {
            listOf(path.toFile())
        }

        return files.map { file -> parseTrace(file.readText()) }
    }
}
