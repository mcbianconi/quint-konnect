package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig

/**
 * Entry point generated tests call. Delegates to a fresh [ReplayRunner] with the console listener;
 * construct a [ReplayRunner] directly for a custom [io.github.mcbianconi.quintkonnect.listener.ReplayListener]
 * or [io.github.mcbianconi.quintkonnect.trace.TraceSource].
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
