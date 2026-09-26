package io.github.mcbianconi.quintkonnect.example.partialstate

import io.github.mcbianconi.quintkonnect.Runner
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import org.junit.jupiter.api.Test

class PartialStateRpsTest {
    @Test
    fun `an ignored status field never fails the check even though it always disagrees with the spec`() {
        Runner.runTest(
            driverFactory = { PartialStateRpsDriver() },
            generatorConfig = RunConfig(
                spec = "src/test/resources/rock-paper-scissors.qnt",
                seed = "42",
                maxSamples = 10,
                maxSteps = 10,
            ),
            testName = "PartialStateRpsDriver",
        )
    }
}
