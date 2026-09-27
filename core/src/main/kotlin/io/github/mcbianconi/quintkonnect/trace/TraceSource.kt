package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
import java.nio.file.Files
import java.nio.file.Path

/**
 * Produces the traces a replay run replays for a [GeneratorConfig].
 *
 * [TraceGenerator] (invoking the `quint` CLI) is the default; other implementations can plug in
 * saved traces, e.g. replaying a `.itf.json` file, or a stub for tests.
 */
public fun interface TraceSource {
    public fun generate(config: GeneratorConfig): List<ItfTrace>
}

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.replay` Gradle property, resolved to an absolute path against the project directory.
internal const val REPLAY_PROPERTY: String = "quintkonnect.replay"

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) to
// generateQuintTraces' output directory (qk-adm2) when that task is wired up: a driver's saved
// traces live at "<this>/<testName>/". Absent for setups that don't go through the plugin (e.g.
// `example`), which keep invoking `quint` at test time as before.
internal const val TRACES_DIR_PROPERTY: String = "quintkonnect.tracesDir"

/**
 * [io.github.mcbianconi.quintkonnect.ReplayRunner]'s default [TraceSource] for [testName]:
 * [ItfFileTraceSource] over [REPLAY_PROPERTY] when that system property is set (resolving a
 * relative path the same way a relative `spec` resolves, via [resolveSpec]); otherwise
 * [TracesDirTraceSource] over "[TRACES_DIR_PROPERTY]/[testName]" when that directory exists;
 * otherwise [TraceGenerator]. An explicit `-Pquint.replay` always wins over a cached traces
 * directory, matching its "reproduce this one saved trace" intent.
 */
internal fun defaultTraceSource(
    testName: String,
    replayPath: String? = System.getProperty(REPLAY_PROPERTY),
    tracesDir: String? = System.getProperty(TRACES_DIR_PROPERTY),
): TraceSource {
    if (replayPath != null) return ItfFileTraceSource(Path.of(resolveSpec(replayPath)))
    if (tracesDir != null) {
        val dir = Path.of(tracesDir, testName)
        if (Files.isDirectory(dir)) return TracesDirTraceSource(dir)
    }
    return TraceGenerator
}
