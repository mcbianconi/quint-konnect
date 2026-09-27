package io.github.mcbianconi.quintkonnect.listener

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.nondet.NondetPicks
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun capture(verbosity: Int = 0, useColor: Boolean = false, block: (ConsoleReplayListener) -> Unit): String {
        val buffer = ByteArrayOutputStream()
        block(ConsoleReplayListener(verbosity, PrintStream(buffer), useColor))
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
    fun `onTraceFailed prints FAIL and the reproduce seed line naming the trace`() {
        val output = capture { it.onTraceFailed(2, config, AssertionError("boom")) }

        assertTrue(output.contains("[FAIL] trace 3"))
        assertTrue(output.contains("Reproduce this error with `QUINT_SEED=12345`"))
    }

    @Test
    fun `onTraceFailureSaved prints the saved path and replay command`() {
        val output = capture {
            it.onTraceFailureSaved(0, "MyTest", java.nio.file.Path.of("build/quint-konnect/failures/MyTest-trace1.itf.json"), "./gradlew test --tests '*MyTest*' -Pquint.replay=build/quint-konnect/failures/MyTest-trace1.itf.json")
        }

        assertTrue(output.contains("Saved failing trace to build/quint-konnect/failures/MyTest-trace1.itf.json"))
        assertTrue(output.contains("./gradlew test --tests '*MyTest*' -Pquint.replay="))
    }

    @Test
    fun `onTraceFailureSaved prints nothing when the file or command is null`() {
        val output = capture { it.onTraceFailureSaved(0, "MyTest", null, null) }

        assertTrue(output.isEmpty())
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

    @Test
    fun `no ANSI codes when colour is disabled`() {
        val output = capture(useColor = false) { it.onRunFinished("passing test", config, null) }

        assertFalse(output.contains("\u001B["))
    }

    @Test
    fun `ANSI codes present when colour is enabled`() {
        val output = capture(useColor = true) { it.onRunFinished("passing test", config, null) }

        assertTrue(output.contains("\u001B["))
    }

    @Test
    fun `QUINT_COLOR=always wins over no console and NO_COLOR`() {
        assertTrue(resolveUseColor(noColor = "1", quintColor = "always", hasConsole = false))
    }

    @Test
    fun `QUINT_COLOR=never wins over a real console and no NO_COLOR`() {
        assertFalse(resolveUseColor(noColor = null, quintColor = "never", hasConsole = true))
    }

    @Test
    fun `NO_COLOR disables colour regardless of its value when QUINT_COLOR is unset`() {
        assertFalse(resolveUseColor(noColor = "0", quintColor = null, hasConsole = true))
    }

    @Test
    fun `an empty NO_COLOR does not disable colour`() {
        assertTrue(resolveUseColor(noColor = "", quintColor = null, hasConsole = true))
    }

    @Test
    fun `no console disables colour when neither env var applies`() {
        assertFalse(resolveUseColor(noColor = null, quintColor = null, hasConsole = false))
    }

    @Test
    fun `a real console with no overrides enables colour`() {
        assertTrue(resolveUseColor(noColor = null, quintColor = null, hasConsole = true))
    }

    @Test
    fun `an unrecognized QUINT_COLOR value falls back to the console and NO_COLOR check`() {
        assertFalse(resolveUseColor(noColor = null, quintColor = "sometimes", hasConsole = false))
        assertTrue(resolveUseColor(noColor = null, quintColor = "sometimes", hasConsole = true))
    }

    @Test
    fun `resolveVerbosity prefers the quintkonnect verbose property over QUINT_VERBOSE`() {
        assertEquals(2, resolveVerbosity(verboseProperty = "2", quintVerbose = "1"))
    }

    @Test
    fun `resolveVerbosity falls back to QUINT_VERBOSE then 0`() {
        assertEquals(1, resolveVerbosity(verboseProperty = null, quintVerbose = "1"))
        assertEquals(0, resolveVerbosity(verboseProperty = null, quintVerbose = null))
    }

    @Test
    fun `resolveVerbosity ignores a non-numeric property and falls through`() {
        assertEquals(1, resolveVerbosity(verboseProperty = "not-a-number", quintVerbose = "1"))
    }
}
