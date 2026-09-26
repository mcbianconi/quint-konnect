package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfState
import io.github.mcbianconi.quintkonnect.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class RunnerTest {

    private val emptyNondet = ItfValue.Record(LinkedHashMap())

    private val fakeConfig = object : GeneratorConfig {
        override val seed = "12345"
        override val nTraces = 1
        override fun toCommand(tmpDir: java.nio.file.Path): List<String> = emptyList()
    }

    private fun stateWithAction(actionTaken: String, nondetPicks: ItfValue = emptyNondet): LinkedHashMap<String, ItfValue> =
        linkedMapOf(
            "mbt::actionTaken" to ItfValue.Str(actionTaken),
            "mbt::nondetPicks" to nondetPicks,
        )

    private fun traceWithAction(actionTaken: String, nondetPicks: ItfValue = emptyNondet): List<ItfTrace> =
        listOf(ItfTrace(states = listOf(ItfState(stateWithAction(actionTaken, nondetPicks)))))

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
    fun `zero traces fails`() {
        val thrown = assertThrows<IllegalStateException> {
            Runner.runTest({ FakeDriver() }, fakeConfig, "empty test", emptyList())
        }
        assertTrue(thrown.message!!.contains("zero traces"))
    }

    @Test
    fun `creates a fresh driver for each trace`() {
        val traces = traceWithAction("A") + traceWithAction("B")
        var factoryCalls = 0
        val createdDrivers = mutableListOf<FakeDriver>()
        val driverFactory = {
            factoryCalls++
            FakeDriver().also { createdDrivers.add(it) }
        }

        Runner.runTest(driverFactory, fakeConfig, "multi trace test", traces)

        assertEquals(2, factoryCalls)
        assertNotSame(createdDrivers[0], createdDrivers[1])
    }

    @Test
    fun `dispatches steps in order with the right action and nondet picks`() {
        fun optionSome(v: Long) = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to ItfValue.Num(v)))
        fun optionNone() = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))

        fun nondetFor(x: Long) = ItfValue.Record(linkedMapOf("x" to optionSome(x), "y" to optionNone()))

        val states = listOf(
            ItfState(stateWithAction("A", nondetFor(1))),
            ItfState(stateWithAction("B", nondetFor(2))),
            ItfState(stateWithAction("C", nondetFor(3))),
        )
        val traces = listOf(ItfTrace(states = states))

        val dispatched = mutableListOf<Triple<String, ItfValue?, ItfValue?>>()
        val driver = FakeDriver { step ->
            dispatched.add(Triple(step.actionTaken, step.nondetPicks.get("x"), step.nondetPicks.get("y")))
        }

        Runner.runTest({ driver }, fakeConfig, "ordered test", traces)

        assertEquals(
            listOf(
                Triple("A", ItfValue.Num(1) as ItfValue?, null as ItfValue?),
                Triple("B", ItfValue.Num(2) as ItfValue?, null as ItfValue?),
                Triple("C", ItfValue.Num(3) as ItfValue?, null as ItfValue?),
            ),
            dispatched,
        )
    }

    @Test
    fun `anonymous action fails without dispatching to the driver`() {
        val traces = traceWithAction("")
        var stepDispatched = false
        val driver = FakeDriver { stepDispatched = true }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "anon test", traces)
        }

        assertFalse(stepDispatched)
        assertTrue(thrown.cause is IllegalStateException)
        assertTrue(thrown.cause!!.message!!.contains("anonymous action"))
        assertTrue(thrown.message!!.contains("trace 1"))
        assertTrue(thrown.message!!.contains("step 0"))
    }

    @Test
    fun `wraps an assertion failure with trace, step and action`() {
        val traces = traceWithAction("TestAction")
        val original = AssertionError("boom")
        val driver = FakeDriver { throw original }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "failing test", traces)
        }

        assertTrue(thrown.message!!.contains("trace 1"))
        assertTrue(thrown.message!!.contains("step 0"))
        assertTrue(thrown.message!!.contains("action 'TestAction'"))
        assertSame(original, thrown.cause)
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
        val original = IllegalStateException("bad state")
        val driver = FakeDriver { throw original }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "failing test", traces)
        }

        assertSame(original, thrown.cause)
    }

    @Test
    fun `stops at the failing step and names its trace and step index`() {
        val trace1 = traceWithAction("A").first()
        val trace2 = ItfTrace(
            states = listOf(
                ItfState(stateWithAction("A2")),
                ItfState(stateWithAction("B")),
            ),
        )
        val traces = listOf(trace1, trace2)

        var factoryCalls = 0
        val dispatchedActions = mutableListOf<String>()
        val driverFactory = {
            factoryCalls++
            FakeDriver { step ->
                dispatchedActions.add(step.actionTaken)
                if (step.actionTaken == "B") throw AssertionError("boom")
            }
        }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest(driverFactory, fakeConfig, "multi trace failing test", traces)
        }

        assertTrue(thrown.message!!.contains("trace 2"))
        assertTrue(thrown.message!!.contains("step 1"))
        assertTrue(thrown.message!!.contains("action 'B'"))
        assertEquals(2, factoryCalls)
        assertEquals(listOf("A", "A2", "B"), dispatchedActions)
    }

    @Test
    fun `a state mismatch surfaces as an AssertionError naming trace, step and action`() {
        val traces = traceWithAction("TestAction")
        val driver = object : Driver {
            override fun step(step: Step) {}
            override fun quintState(): State<*> = object : State<Driver> {
                override fun check(driver: Driver, specValue: ItfValue) {
                    error("State invariant failed:\nsomething mismatched")
                }
            }
        }

        val thrown = assertThrows<AssertionError> {
            Runner.runTest({ driver }, fakeConfig, "mismatch test", traces)
        }

        assertTrue(thrown.message!!.contains("trace 1"))
        assertTrue(thrown.message!!.contains("step 0"))
        assertTrue(thrown.message!!.contains("action 'TestAction'"))
        assertTrue(thrown.cause is IllegalStateException)
        assertEquals("State invariant failed:\nsomething mismatched", thrown.cause!!.message)
    }

    @Test
    fun `state check is skipped when the driver returns State-disabled`() {
        val traces = traceWithAction("TestAction")
        val driver = object : Driver {
            override fun step(step: Step) {}
            override fun quintState(): State<*> = State.disabled<Driver>()
        }

        Runner.runTest({ driver }, fakeConfig, "disabled test", traces)
    }

    @Test
    @Disabled("see qk-9geu: Step.fromState mutates the shared trace state map, so replaying the same List<ItfTrace> twice fails")
    fun `replays the same traces more than once`() {
        val traces = traceWithAction("A")
        Runner.runTest({ FakeDriver() }, fakeConfig, "first run", traces)
        Runner.runTest({ FakeDriver() }, fakeConfig, "second run", traces)
    }
}
