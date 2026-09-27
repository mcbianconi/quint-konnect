package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.parseTrace
import io.github.mcbianconi.quintkonnect.listener.ReplayListener
import io.github.mcbianconi.quintkonnect.trace.FAILURES_DIR_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.ItfFileTraceSource
import io.github.mcbianconi.quintkonnect.trace.TraceSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReplayRunnerTest {

    private val emptyNondet = ItfValue.Record(LinkedHashMap())

    private val fakeConfig = object : GeneratorConfig {
        override val seed = "12345"
        override val nTraces = 1
        override fun toCommand(tmpDir: java.nio.file.Path): List<String> = emptyList()
    }

    private val silentListener = object : ReplayListener {}

    private fun runner(
        traces: List<ItfTrace>,
        listener: ReplayListener = silentListener,
        config: GeneratorConfig = fakeConfig,
    ): ReplayRunner = ReplayRunner(config, TraceSource { traces }, listener)

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

    private class RecordingReplayListener : ReplayListener {
        val events = mutableListOf<String>()
        var stepFailure: Throwable? = null
        var runFailure: Throwable? = null
        var traceFailure: Throwable? = null
        val traceFailedSeeds = mutableListOf<String>()
        val savedFailureFiles = mutableListOf<Path?>()
        val savedReplayCommands = mutableListOf<String?>()

        override fun onRunStarted(testName: String, config: GeneratorConfig) {
            events += "runStarted"
        }

        override fun onTraceStarted(traceIndex: Int) {
            events += "traceStarted:$traceIndex"
        }

        override fun onStep(traceIndex: Int, stepIndex: Int, step: Step) {
            events += "step:$traceIndex:$stepIndex:${step.actionTaken}"
        }

        override fun onStepFailed(traceIndex: Int, stepIndex: Int, step: Step?, failure: Throwable) {
            events += "stepFailed:$traceIndex:$stepIndex"
            stepFailure = failure
        }

        override fun onTraceFinished(traceIndex: Int) {
            events += "traceFinished:$traceIndex"
        }

        override fun onTraceFailed(traceIndex: Int, config: GeneratorConfig, failure: Throwable) {
            events += "traceFailed:$traceIndex"
            traceFailure = failure
            traceFailedSeeds += config.seed
        }

        override fun onTraceFailureSaved(traceIndex: Int, testName: String, failureFile: Path?, replayCommand: String?) {
            events += "traceFailureSaved:$traceIndex"
            savedFailureFiles += failureFile
            savedReplayCommands += replayCommand
        }

        override fun onRunFinished(testName: String, config: GeneratorConfig, failure: Throwable?) {
            events += "runFinished:${failure == null}"
            runFailure = failure
        }
    }

    @Test
    fun `passes through when driver accepts every step`() {
        val traces = traceWithAction("TestAction")
        runner(traces).runTest({ FakeDriver() }, "ok test")
    }

    @Test
    fun `zero traces fails`() {
        val thrown = assertThrows<IllegalStateException> {
            runner(emptyList()).runTest({ FakeDriver() }, "empty test")
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

        runner(traces).runTest(driverFactory, "multi trace test")

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

        runner(traces).runTest({ driver }, "ordered test")

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
            runner(traces).runTest({ driver }, "anon test")
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
            runner(traces).runTest({ driver }, "failing test")
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
            runner(traces).runTest({ driver }, "failing test")
        }

        assertTrue(thrown.message!!.contains("Nondet picks"))
        assertTrue(thrown.message!!.contains("x: 7"))
    }

    @Test
    fun `rethrows the original exception type as the cause`() {
        val traces = traceWithAction("TestAction")
        val original = IllegalStateException("bad state")
        val driver = FakeDriver { throw original }

        val thrown = assertThrows<AssertionError> {
            runner(traces).runTest({ driver }, "failing test")
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
            runner(traces).runTest(driverFactory, "multi trace failing test")
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
            runner(traces).runTest({ driver }, "mismatch test")
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

        runner(traces).runTest({ driver }, "disabled test")
    }

    @Test
    fun `replays the same traces more than once`() {
        val traces = traceWithAction("A")
        runner(traces).runTest({ FakeDriver() }, "first run")
        runner(traces).runTest({ FakeDriver() }, "second run")
    }

    @Test
    fun `fires events in order for a passing run`() {
        val traces = traceWithAction("A") + traceWithAction("B")
        val listener = RecordingReplayListener()

        runner(traces, listener).runTest({ FakeDriver() }, "ordered events")

        assertEquals(
            listOf(
                "runStarted",
                "traceStarted:0",
                "step:0:0:A",
                "traceFinished:0",
                "traceStarted:1",
                "step:1:0:B",
                "traceFinished:1",
                "runFinished:true",
            ),
            listener.events,
        )
    }

    @Test
    fun `fires a step-failed event before throwing the AssertionError`() {
        val traces = traceWithAction("TestAction")
        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }

        val thrown = assertThrows<AssertionError> {
            runner(traces, listener).runTest({ driver }, "failing test")
        }

        assertEquals(
            listOf(
                "runStarted",
                "traceStarted:0",
                "step:0:0:TestAction",
                "stepFailed:0:0",
                "runFinished:false",
            ),
            listener.events,
        )
        assertSame(thrown, listener.stepFailure)
        assertSame(thrown, listener.runFailure)
    }

    @Test
    fun `propagates a trace-generation failure without a run-finished event`() {
        val listener = RecordingReplayListener()
        val failingSource = TraceSource { error("quint failed") }

        assertThrows<IllegalStateException> {
            ReplayRunner(fakeConfig, failingSource, listener).runTest({ FakeDriver() }, "gen failure")
        }

        assertEquals(listOf("runStarted"), listener.events)
    }

    @Test
    fun `traceReplays returns one replay per trace with a 1-based display name and the seed`() {
        val traces = traceWithAction("A") + traceWithAction("B")
        val replays = runner(traces, config = fakeConfig).traceReplays({ FakeDriver() }, "per trace test")

        assertEquals(2, replays.size)
        assertEquals("trace 1 (seed 12345)", replays[0].displayName)
        assertEquals("trace 2 (seed 12345)", replays[1].displayName)
    }

    @Test
    fun `traceReplays generates traces once and calls the driver factory once per run`() {
        var generateCalls = 0
        val traces = traceWithAction("A") + traceWithAction("B")
        val source = TraceSource { generateCalls++; traces }
        var factoryCalls = 0
        val driverFactory = { factoryCalls++; FakeDriver() }

        val replays = ReplayRunner(fakeConfig, source, silentListener).traceReplays(driverFactory, "gen once")
        assertEquals(1, generateCalls)
        assertEquals(0, factoryCalls)

        replays.forEach { it.run() }
        assertEquals(1, generateCalls)
        assertEquals(2, factoryCalls)
    }

    @Test
    fun `each traceReplays run gets a fresh driver`() {
        val traces = traceWithAction("A") + traceWithAction("B")
        val createdDrivers = mutableListOf<FakeDriver>()
        val driverFactory = { FakeDriver().also { createdDrivers.add(it) } }

        runner(traces).traceReplays(driverFactory, "fresh driver test").forEach { it.run() }

        assertEquals(2, createdDrivers.size)
        assertNotSame(createdDrivers[0], createdDrivers[1])
    }

    @Test
    fun `a failing trace does not stop other traces from running`() {
        val trace1 = ItfTrace(states = listOf(ItfState(stateWithAction("A"))))
        val trace2 = ItfTrace(states = listOf(ItfState(stateWithAction("B"))))
        val traces = listOf(trace1, trace2)

        val dispatched = mutableListOf<String>()
        val driverFactory = {
            FakeDriver { step ->
                dispatched += step.actionTaken
                if (step.actionTaken == "A") throw AssertionError("boom")
            }
        }

        val replays = runner(traces).traceReplays(driverFactory, "independent traces")

        assertThrows<AssertionError> { replays[0].run() }
        replays[1].run()

        assertEquals(listOf("A", "B"), dispatched)
    }

    @Test
    fun `a failing trace fires onTraceFailed with the wrapped assertion and the config`() {
        val traces = traceWithAction("TestAction")
        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }

        val replays = runner(traces, listener).traceReplays({ driver }, "failing trace test")
        val thrown = assertThrows<AssertionError> { replays[0].run() }

        assertSame(thrown, listener.traceFailure)
        assertTrue(listener.events.contains("traceFailed:0"))
        assertFalse(listener.events.any { it.startsWith("runFinished") })
    }

    @Test
    fun `a failing trace is saved as ITF JSON and fires onTraceFailureSaved after onTraceFailed`(@TempDir dir: Path) {
        val traces = traceWithAction("TestAction")
        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }

        withFailuresDir(dir) {
            val replays = runner(traces, listener).traceReplays({ driver }, "MyTest")
            assertThrows<AssertionError> { replays[0].run() }
        }

        assertEquals(listOf("traceFailed:0", "traceFailureSaved:0"), listener.events.filter { "traceFail" in it })
        val savedFile = listener.savedFailureFiles.single()!!
        assertEquals("MyTest-trace1.itf.json", savedFile.fileName.toString())
        assertEquals(traces[0], parseTrace(Files.readString(savedFile)))
        assertTrue(listener.savedReplayCommands.single()!!.contains("-Pquint.replay="))
    }

    @Test
    fun `replaying a saved trace skips re-saving and points the command at the input file`(@TempDir dir: Path) {
        val inputFile = dir.resolve("input.itf.json")
        Files.writeString(inputFile, """{"states": [{"mbt::actionTaken": "TestAction", "mbt::nondetPicks": {}}]}""")

        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }
        val replayRunner = ReplayRunner(fakeConfig, ItfFileTraceSource(inputFile), listener)

        val failuresDir = dir.resolve("failures")
        withFailuresDir(failuresDir) {
            val replays = replayRunner.traceReplays({ driver }, "MyTest")
            assertThrows<AssertionError> { replays[0].run() }
        }

        assertEquals(inputFile, listener.savedFailureFiles.single())
        assertTrue(listener.savedReplayCommands.single()!!.contains(inputFile.toString()))
        assertFalse(Files.exists(failuresDir))
    }

    @Test
    fun `a failure saving the trace does not mask the original failure and reports it as suppressed`() {
        val traces = traceWithAction("TestAction")
        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }

        // A regular file where writeFailureTrace expects to create a directory: Files.createDirectories fails.
        val notADirectory = Files.createTempFile("quint-konnect-test-", ".tmp")
        notADirectory.toFile().deleteOnExit()

        val thrown = withFailuresDir(notADirectory) {
            val replays = runner(traces, listener).traceReplays({ driver }, "MyTest")
            assertThrows<AssertionError> { replays[0].run() }
        }

        assertEquals(listOf(null), listener.savedFailureFiles)
        assertEquals(listOf(null), listener.savedReplayCommands)
        assertTrue(thrown.suppressed.isNotEmpty())
    }

    private fun <T> withFailuresDir(dir: Path, block: () -> T): T {
        val previous = System.getProperty(FAILURES_DIR_PROPERTY)
        System.setProperty(FAILURES_DIR_PROPERTY, dir.toString())
        try {
            return block()
        } finally {
            if (previous == null) System.clearProperty(FAILURES_DIR_PROPERTY) else System.setProperty(FAILURES_DIR_PROPERTY, previous)
        }
    }

    @Test
    fun `every failing trace fires its own onTraceFailed with the reproduce seed`() {
        val traces = traceWithAction("A") + traceWithAction("B")
        val listener = RecordingReplayListener()
        val driver = FakeDriver { throw IllegalStateException("boom") }
        val config = object : GeneratorConfig by fakeConfig {
            override val seed = "cafe"
        }

        val replays = ReplayRunner(config, TraceSource { traces }, listener).traceReplays({ driver }, "both fail")

        assertThrows<AssertionError> { replays[0].run() }
        assertThrows<AssertionError> { replays[1].run() }

        assertEquals(listOf("traceFailed:0", "traceFailed:1"), listener.events.filter { it.startsWith("traceFailed") })
        assertEquals(listOf("cafe", "cafe"), listener.traceFailedSeeds)
        assertFalse(listener.events.any { it.startsWith("runFinished") })
    }

    @Test
    fun `traceReplays with zero traces returns a single replay that fails`() {
        val listener = RecordingReplayListener()
        val replays = ReplayRunner(fakeConfig, TraceSource { emptyList() }, listener)
            .traceReplays({ FakeDriver() }, "empty test")

        assertEquals(1, replays.size)

        val thrown = assertThrows<IllegalStateException> { replays[0].run() }
        assertTrue(thrown.message!!.contains("zero traces"))
        assertTrue(listener.events.contains("runFinished:false"))
        assertSame(thrown, listener.runFailure)
    }

    @Test
    fun `traceReplays propagates a trace-generation failure without returning any replay`() {
        val listener = RecordingReplayListener()
        val failingSource = TraceSource { error("quint failed") }

        assertThrows<IllegalStateException> {
            ReplayRunner(fakeConfig, failingSource, listener).traceReplays({ FakeDriver() }, "gen failure")
        }

        assertEquals(listOf("runStarted"), listener.events)
    }

    @Test
    fun `a passing trace does not fire onTraceFailed or onRunFinished`() {
        val traces = traceWithAction("A")
        val listener = RecordingReplayListener()

        runner(traces, listener).traceReplays({ FakeDriver() }, "passing trace test").forEach { it.run() }

        assertFalse(listener.events.any { it.startsWith("traceFailed") || it.startsWith("runFinished") })
    }
}
