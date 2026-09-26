package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig

/**
 * Kept for binary compatibility with already-compiled generated code that still calls [runTest]
 * for a whole batch of traces in one go. Current KSP output instead constructs a [ReplayRunner]
 * and calls [ReplayRunner.traceReplays] directly, to get one [TraceReplay] per trace (e.g. for a
 * JUnit `@TestFactory`). Construct a [ReplayRunner] directly for a custom
 * [io.github.mcbianconi.quintkonnect.listener.ReplayListener] or
 * [io.github.mcbianconi.quintkonnect.trace.TraceSource].
 */
public object Runner {

    public fun <D : Driver> runTest(
        driverFactory: () -> D,
        generatorConfig: GeneratorConfig,
        testName: String,
    ) {
        ReplayRunner(generatorConfig).runTest(driverFactory, testName)
    }
}
