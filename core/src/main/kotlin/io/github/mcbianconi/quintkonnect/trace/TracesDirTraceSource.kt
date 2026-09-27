package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.parseTrace
import java.nio.file.Files
import java.nio.file.Path

private val trailingDigitsRegex = Regex("\\d+")

// Matches the last run of digits in the file name, not the first (mirrors ItfFileTraceSource.kt's
// sequenceNumber): a testName can itself contain digits (e.g. "Foo2Test").
private fun sequenceNumber(fileName: String): Long? =
    trailingDigitsRegex.findAll(fileName).lastOrNull()?.value?.toLongOrNull()

// The name generateQuintTraces (gradle-plugin's GenerateQuintTracesTask, qk-adm2) writes the
// resolved per-driver seed under, next to the driver's trace files. Not a ".itf.json" file, so it's
// never picked up as a trace below.
internal const val SEED_FILE_NAME: String = "seed.txt"

// The name generateQuintTraces writes instead of trace files when `quint` exited non-zero for a
// driver (an invariant violation or any other CLI failure): its content is thrown verbatim by
// [generate] below, so the failure still surfaces as a JUnit test failure at replay time, the same
// way it would if `quint` had failed at test time (TraceGenerator.kt).
internal const val ERROR_FILE_NAME: String = "error.txt"

/**
 * [io.github.mcbianconi.quintkonnect.ReplayRunner]'s trace source when generateQuintTraces' output
 * directory has a subdirectory for the running test (qk-adm2): replays every "*.itf.json" file
 * directly inside [dir] (not recursive), read in numeric order by the last run of digits in each
 * file name (matching [TraceGenerator]'s and `FailureTraceWriter`'s naming). Only ".itf.json" files
 * are read as traces, so [SEED_FILE_NAME]/[ERROR_FILE_NAME] alongside them are ignored.
 *
 * If [ERROR_FILE_NAME] is present, [generate] throws with its content instead of returning traces:
 * `quint` failed for this driver when generateQuintTraces ran, and that failure is reproduced here
 * instead of failing the whole build at generation time.
 */
internal class TracesDirTraceSource(internal val dir: Path) : TraceSource {

    override fun generate(config: GeneratorConfig): List<ItfTrace> {
        val errorFile = dir.resolve(ERROR_FILE_NAME)
        if (Files.isRegularFile(errorFile)) error(Files.readString(errorFile))

        val files = dir.toFile()
            .listFiles { f -> f.isFile && f.name.endsWith(".itf.json") }
            ?.sortedWith(compareBy({ sequenceNumber(it.name) }, { it.name }))
            ?: emptyList()
        return files.map { file -> parseTrace(file.readText()) }
    }

    // Read for display only (ReplayRunner.kt): the seed baked into the generated RunConfig/
    // TestConfig at test time isn't necessarily the seed generateQuintTraces actually ran `quint`
    // with (e.g. when neither is pinned, each resolves its own random fallback independently), so
    // the "reproduce with seed X" message would otherwise name the wrong seed for these traces.
    internal fun recordedSeed(): String? =
        dir.resolve(SEED_FILE_NAME).takeIf { Files.isRegularFile(it) }?.let { Files.readString(it).trim() }
}
