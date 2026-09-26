package io.github.mcbianconi.quintkonnect.example.buggy

import io.github.mcbianconi.quintkonnect.Runner
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BuggyRockPaperScissorsTest {
    @Test
    fun `catches the swapped winner bug`() {
        assertThrows<AssertionError> {
            Runner.runTest(
                driverFactory = { BuggyRockPaperScissorsDriver() },
                generatorConfig = RunConfig(
                    spec = "src/test/resources/rock-paper-scissors.qnt",
                    seed = "42",
                    maxSamples = 10,
                    maxSteps = 10,
                ),
                testName = "BuggyRockPaperScissorsDriver",
            )
        }
    }
}
