package io.github.mcbianconi.quintkonnect

/**
 * One trace out of a [ReplayRunner.traceReplays] call: [displayName] identifies it for reporting
 * (e.g. as a JUnit `DynamicTest` name) and [run] replays it against a fresh driver.
 *
 * Trace generation already happened when the [ReplayRunner.traceReplays] call returned this list;
 * [run] only replays the steps of this one trace, so failures in other [TraceReplay]s don't affect
 * it.
 */
public class TraceReplay internal constructor(
    public val displayName: String,
    private val body: () -> Unit,
) {
    public fun run() {
        body()
    }
}
