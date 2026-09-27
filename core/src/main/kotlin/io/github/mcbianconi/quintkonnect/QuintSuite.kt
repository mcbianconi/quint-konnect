package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.listener.ConsoleReplayListener
import io.github.mcbianconi.quintkonnect.listener.ReplayListener

/**
 * The runner-neutral description of a driver's generated traces.
 *
 * KSP generates one `object <Driver>QuintSuite : QuintSuite` per `@QuintRun`/`@QuintTest` driver,
 * holding the `RunConfig`/`TestConfig` construction a hand-written [ReplayRunner] call would
 * otherwise need. A test-framework adapter iterates this instead of talking to [ReplayRunner]
 * itself: the generated JUnit `<Driver>QuintRunTest`/`<Driver>QuintTestTest` class does this
 * today, and a Kotest adapter can do the same later.
 */
public interface QuintSuite {

    /** This driver's simple class name, matching [ReplayRunner.traceReplays]'s own `testName`. */
    public val name: String

    /** Generates this suite's traces and returns one [TraceReplay] per trace, reported to [listener]. */
    public fun traceReplays(listener: ReplayListener = ConsoleReplayListener()): List<TraceReplay>
}
