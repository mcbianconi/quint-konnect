package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace
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

/**
 * [io.github.mcbianconi.quintkonnect.ReplayRunner]'s default [TraceSource]: [ItfFileTraceSource]
 * over [REPLAY_PROPERTY] when that system property is set (resolving a relative path the same way
 * a relative `spec` resolves, via [resolveSpec]), otherwise [TraceGenerator].
 */
internal fun defaultTraceSource(replayPath: String? = System.getProperty(REPLAY_PROPERTY)): TraceSource =
    replayPath?.let { ItfFileTraceSource(Path.of(resolveSpec(it))) } ?: TraceGenerator
