package io.github.mcbianconi.quintkonnect.integrationtests.invariants

import io.github.mcbianconi.quintkonnect.Runner
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnsafeCounterInvariantTest {
    @Test
    fun `fails with a clear message naming the violated invariant`() {
        val exception = assertThrows<IllegalStateException> {
            Runner.runTest(
                driverFactory = { UnsafeCounterDriver() },
                generatorConfig = RunConfig(
                    spec = "src/test/resources/invariants/unsafe_counter.qnt",
                    seed = "42",
                    maxSamples = 1,
                    maxSteps = 5,
                    invariants = listOf("safe"),
                ),
                testName = "UnsafeCounterDriver",
            )
        }

        val message = exception.message.orEmpty()
        assert(message.contains("Quint invariant violated: safe")) {
            "Expected a clear invariant violation message, got: $message"
        }
        assert(message.contains("seed 42")) { "Expected the seed in the message, got: $message" }
    }
}
