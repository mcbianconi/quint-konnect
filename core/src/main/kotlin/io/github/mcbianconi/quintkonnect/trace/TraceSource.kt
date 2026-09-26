package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfTrace

/**
 * Produces the traces a replay run replays for a [GeneratorConfig].
 *
 * [TraceGenerator] (invoking the `quint` CLI) is the default; other implementations can plug in
 * saved traces, e.g. replaying a `.itf.json` file, or a stub for tests.
 */
public fun interface TraceSource {
    public fun generate(config: GeneratorConfig): List<ItfTrace>
}
