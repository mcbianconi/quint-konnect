package io.github.mcbianconi.quintkonnect.listener

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.nondet.NondetPicks
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsoleReplayListenerTest {

    private val config = object : GeneratorConfig {
        override val seed = "12345"
        override val nTraces = 7
        override fun toCommand(tmpDir: java.nio.file.Path): List<String> = emptyList()
    }

    private fun capture(verbosity: Int = 0, block: (ConsoleReplayListener) -> Unit): String {
        val buffer = ByteArrayOutputStream()
        block(ConsoleReplayListener(verbosity, PrintStream(buffer)))
        return buffer.toString()
    }

    @Test
    fun `prints OK and no seed line on success`() {
        val output = capture { it.onRunFinished("passing test", config, null) }

        assertTrue(output.contains("[OK] passing test"))
        assertFalse(output.contains("Reproduce"))
    }

    @Test
    fun `prints FAIL and the reproduce seed line on failure`() {
        val output = capture { it.onRunFinished("failing test", config, AssertionError("boom")) }

        assertTrue(output.contains("[FAIL] failing test"))
        assertTrue(output.contains("Reproduce this error with `QUINT_SEED=12345`"))
    }

    @Test
    fun `prints the run header naming the test and trace count`() {
        val output = capture { it.onRunStarted("my test", config) }

        assertTrue(output.contains("Running model based tests for my test"))
        assertTrue(output.contains("Generating 7 traces"))
        assertTrue(output.contains("12345"))
    }

    @Test
    fun `trace and step messages are hidden below verbosity 1`() {
        val step = Step("A", NondetPicks.empty(), ItfValue.Record(LinkedHashMap()))
        val output = capture(verbosity = 0) {
            it.onTraceStarted(0)
            it.onStep(0, 0, step)
        }

        assertTrue(output.isEmpty())
    }

    @Test
    fun `trace and step messages print at verbosity 1`() {
        val step = Step("TestAction", NondetPicks.empty(), ItfValue.Record(LinkedHashMap()))
        val output = capture(verbosity = 1) {
            it.onTraceStarted(0)
            it.onStep(0, 0, step)
        }

        assertTrue(output.contains("[Trace 1]"))
        assertTrue(output.contains("[Step 0]"))
        assertTrue(output.contains("TestAction"))
    }

    @Test
    fun `raw state is hidden below verbosity 2 and printed at verbosity 2`() {
        val rawState = ItfState(linkedMapOf("mbt::actionTaken" to ItfValue.Str("A")))

        val atOne = capture(verbosity = 1) { it.onStepStarted(0, 0, rawState) }
        assertFalse(atOne.contains("Deriving step from"))

        val atTwo = capture(verbosity = 2) { it.onStepStarted(0, 0, rawState) }
        assertTrue(atTwo.contains("Deriving step from"))
        assertTrue(atTwo.contains("mbt::actionTaken"))
    }
}
