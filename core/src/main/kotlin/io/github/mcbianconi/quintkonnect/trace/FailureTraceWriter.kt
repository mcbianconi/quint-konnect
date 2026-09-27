package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.ItfValueSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt); falls back to
// PROJECT_DIR_PROPERTY/build/quint-konnect/failures, then the working directory, for setups that
// don't go through the plugin (e.g. `example`, see AGENTS.md).
internal const val FAILURES_DIR_PROPERTY: String = "quintkonnect.failuresDir"

internal fun resolveFailuresDir(
    override: String? = System.getProperty(FAILURES_DIR_PROPERTY),
    projectDir: String? = System.getProperty(PROJECT_DIR_PROPERTY),
): Path =
    override?.let { Path.of(it) } ?: Path.of(projectDir ?: ".", "build", "quint-konnect", "failures")

// The inverse of parseTrace/ItfValueSerializer.fromJsonElement: round-trips each field through
// ItfValueSerializer.toJsonElement. A trace-level `#meta` (if quint wrote one) isn't preserved:
// parseTrace already strips it on the way in, so there's nothing left to write back.
internal fun ItfTrace.toItfJson(): String {
    val root = buildJsonObject {
        putJsonArray("vars") { vars.forEach { add(JsonPrimitive(it)) } }
        putJsonArray("states") {
            states.forEach { state ->
                add(
                    buildJsonObject {
                        for ((key, value) in state.value) put(key, ItfValueSerializer.toJsonElement(value))
                    },
                )
            }
        }
    }
    return Json.encodeToString(JsonObject.serializer(), root)
}

// "/" is the only character sanitized: a fully qualified test name shouldn't be able to escape
// the failures directory. No Windows-reserved-character handling (docs/decisions/no-windows-support.md).
private fun sanitizeFileName(name: String): String = name.replace("/", "_")

/**
 * Writes [trace] as ITF JSON to `<failuresDir>/<testName>-trace<traceIndex + 1>.itf.json`
 * ([traceIndex] 1-based in the file name, matching [io.github.mcbianconi.quintkonnect.TraceReplay]'s
 * "trace N" display name), replacing any file already there, and returns that path.
 *
 * Written to a temp file first and moved into place atomically, so a concurrent reader (or a
 * concurrent write for a same-named trace from another parallel run) never observes a partially
 * written file.
 *
 * @throws IOException if creating the directory or writing the file fails.
 */
internal fun writeFailureTrace(
    testName: String,
    traceIndex: Int,
    trace: ItfTrace,
    dir: Path = resolveFailuresDir(),
): Path {
    Files.createDirectories(dir)
    val file = dir.resolve("${sanitizeFileName(testName)}-trace${traceIndex + 1}.itf.json")
    val tmp = Files.createTempFile(dir, file.fileName.toString(), ".tmp")
    try {
        Files.writeString(tmp, trace.toItfJson())
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } finally {
        Files.deleteIfExists(tmp)
    }
    return file
}

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) to that Test task's
// Gradle path (e.g. ":test" or ":example:test"), so replayCommand()'s printed command targets the
// right task in a multi-project build. Falls back to plain "test" when unset (e.g. `example`).
internal const val TEST_TASK_PATH_PROPERTY: String = "quintkonnect.testTaskPath"

/**
 * The `./gradlew ... -Pquint.replay=...` command a user can copy to replay [file] for [testName].
 *
 * `--tests` is matched with a `*testName*` wildcard rather than the exact generated test class
 * name (e.g. `FooDriverQuintRunTest`, from the KSP-generated `@TestFactory`): [testName] is the
 * driver class name, one KSP naming step removed from the class JUnit actually runs, and this
 * function has no way to know which of `@QuintRun`'s/`@QuintTest`'s suffixes applies.
 */
internal fun replayCommand(
    testName: String,
    file: Path,
    taskPath: String? = System.getProperty(TEST_TASK_PATH_PROPERTY),
    projectDir: String? = System.getProperty(PROJECT_DIR_PROPERTY),
): String {
    val absoluteFile = file.toAbsolutePath()
    val displayPath = projectDir
        ?.let { Path.of(it).toAbsolutePath() }
        ?.takeIf { absoluteFile.startsWith(it) }
        ?.let { it.relativize(absoluteFile).toString() }
        ?: absoluteFile.toString()
    return "./gradlew ${taskPath ?: "test"} --tests '*$testName*' -Pquint.replay=$displayPath"
}
