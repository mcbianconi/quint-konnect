package io.github.mcbianconi.quintkonnect.example.buggy

import io.github.mcbianconi.quintkonnect.ReplayRunner
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// qk-a8ay against real quint: the shrinkQuintTraces task sets this property; here it's set by hand
// since the example wires KSP without the Gradle plugin.
class BuggyRockPaperScissorsShrinkTest {
    @Test
    fun `shrinks the swapped winner bug to a shorter failing trace`() {
        System.setProperty("quintkonnect.shrink", "true")
        val error = try {
            val replays = ReplayRunner(
                RunConfig(spec = "src/test/resources/rock-paper-scissors.qnt", seed = "42", maxSamples = 10, maxSteps = 10),
            ).traceReplays({ BuggyRockPaperScissorsDriver() }, "BuggyRockPaperScissorsDriver")
            assertThrows<AssertionError> { replays.forEach { it.run() } }
        } finally {
            System.clearProperty("quintkonnect.shrink")
        }
        // Seed 42 on quint 0.32.0: the first failing trace fails at step 5, and --max-steps 2 with
        // the same seed already produces one failing at step 2.
        assertTrue(error.message!!.startsWith("Shrunk failing trace: it failed at step 5;"), error.message)
        assertTrue("--max-steps 2, trace 1 fails at step 2" in error.message!!, error.message)
    }
}
