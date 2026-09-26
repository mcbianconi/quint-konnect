package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfState
import io.github.mcbianconi.quintkonnect.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

class RunnerTest {

    private val emptyNondet = ItfValue.Record(LinkedHashMap())

    private val fakeConfig = object : GeneratorConfig {
        override val seed = "12345"
        override val nTraces = 1
        override fun toCommand(tmpDir: Path): List<String> = emptyList()
    }

    private fun traceWithAction(actionTaken: String, nondetPicks: ItfValue = emptyNondet): List<ItfTrace> {
        val state = linkedMapOf<String, ItfValue>(
            "mbt::actionTaken" to ItfValue.Str(actionTaken),
            "mbt::nondetPicks" to nondetPicks,
        )
        return listOf(ItfTrace(states = listOf(ItfState(state))))
    }

    private class FakeDriver(private val onStep: (Step) -> Unit = {}) : Driver {
        override fun step(step: Step) = onStep(step)
    }

    private fun captureStderr(block: () -> Unit): String {
        val original = System.err
        val buffer = ByteArrayOutputStream()
        System.setErr(PrintStream(buffer))
        try {
            block()
        } finally {
            System.setErr(original)
        }
        return buffer.toString()
    }

    @Test
    fun `passes through when driver accepts every step`() {
        val traces = traceWithAction("TestAction")
        Runner.runTest({ FakeDriver() }, fakeConfig, "ok test", traces)
    }

    @Test
    fun `wraps an assertion failure with trace, step and action`() {
        val traces = traceWithAction("TestAction")
        val driver = FakeDriver { throw AssertionError("boom") }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "failing test", traces)
        }

        assertTrue(thrown.message!!.contains("trace 1"))
        assertTrue(thrown.message!!.contains("step 0"))
        assertTrue(thrown.message!!.contains("action 'TestAction'"))
        assertEquals("boom", thrown.cause!!.message)
        assertTrue(thrown.cause is AssertionError)
    }

    @Test
    fun `includes nondet picks in the failure message when present`() {
        val nondet = ItfValue.Record(linkedMapOf("x" to ItfValue.Num(7)))
        val traces = traceWithAction("TestAction", nondet)
        val driver = FakeDriver { throw AssertionError("boom") }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "failing test", traces)
        }

        assertTrue(thrown.message!!.contains("Nondet picks"))
        assertTrue(thrown.message!!.contains("x: 7"))
    }

    @Test
    fun `prints the reproduce seed message on an assertion failure`() {
        val traces = traceWithAction("TestAction")
        val driver = FakeDriver { throw AssertionError("boom") }

        val output = captureStderr {
            assertThrows<AssertionError> {
                Runner.runTest({ driver }, fakeConfig, "failing test", traces)
            }
        }

        assertTrue(output.contains("Reproduce this error with `QUINT_SEED=12345`"))
    }

    @Test
    fun `rethrows the original exception type as the cause`() {
        val traces = traceWithAction("TestAction")
        val driver = FakeDriver { throw IllegalStateException("bad state") }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "failing test", traces)
        }

        assertTrue(thrown.cause is IllegalStateException)
    }
}
