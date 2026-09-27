package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.listener.ReplayListener
import io.github.mcbianconi.quintkonnect.trace.FAILURES_DIR_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.MaxStepsConfig
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import io.github.mcbianconi.quintkonnect.trace.SHRINK_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.TraceSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

// qk-a8ay: ReplayRunner.traceReplays' shrinking, with a stub TraceSource standing in for quint:
// it returns a longer or shorter trace depending on the --max-steps the shrink search asks for.
class ShrinkTest {

    private val config = RunConfig(spec = "unused.qnt", seed = "0x1")

    private fun trace(vararg actions: String) = ItfTrace(
        states = actions.map { action ->
            ItfState(
                linkedMapOf(
                    "mbt::actionTaken" to ItfValue.Str(action),
                    "mbt::nondetPicks" to ItfValue.Record(LinkedHashMap()),
                ),
            )
        },
    )

    // Unshrunk, the only trace fails at step 4; with --max-steps 2 or more, a trace failing at step
    // 2 appears; below that, nothing fails.
    private val requestedMaxSteps = mutableListOf<Int?>()
    private val source = TraceSource { cfg ->
        val maxSteps = (cfg as? MaxStepsConfig)?.maxSteps
        requestedMaxSteps += maxSteps
        when {
            maxSteps == null -> listOf(trace("init", "ok", "ok", "ok", "bad"))
            maxSteps >= 2 -> listOf(trace("init", "ok", "bad"))
            else -> listOf(trace(*Array(maxSteps + 1) { if (it == 0) "init" else "ok" }))
        }
    }

    private class FailOnBad : Driver {
        override fun step(step: Step) {
            check(step.actionTaken != "bad") { "bad action" }
        }
    }

    private fun <T> withProperties(vararg props: Pair<String, String>, block: () -> T): T {
        val previous = props.associate { (k, _) -> k to System.getProperty(k) }
        props.forEach { (k, v) -> System.setProperty(k, v) }
        try {
            return block()
        } finally {
            previous.forEach { (k, v) -> if (v == null) System.clearProperty(k) else System.setProperty(k, v) }
        }
    }

    @Test
    fun `reports and saves the shortest failing trace when shrinking is on`(@TempDir dir: Path) {
        val saved = mutableListOf<Path?>()
        val listener = object : ReplayListener {
            override fun onTraceFailureSaved(traceIndex: Int, testName: String, failureFile: Path?, replayCommand: String?) {
                saved += failureFile
            }
        }

        val error = withProperties(SHRINK_PROPERTY to "true", FAILURES_DIR_PROPERTY to dir.toString()) {
            val replay = ReplayRunner(config, source, listener).traceReplays({ FailOnBad() }, "Drv").single()
            assertThrows<AssertionError> { replay.run() }
        }

        assertEquals(listOf(null, 0, 1, 2), requestedMaxSteps)
        assertTrue("--max-steps 2" in error.message!!, error.message)
        assertTrue("failed at step 4" in error.message!!, error.message)
        assertTrue("fails at step 2" in error.message!!, error.message)
        val file = saved.single()!!
        assertEquals("Drv-shrunk-trace1.itf.json", file.fileName.toString())
        assertTrue(Files.readString(file).contains("\"bad\""))
    }

    @Test
    fun `leaves the failure unchanged when shrinking is off`(@TempDir dir: Path) {
        val error = withProperties(FAILURES_DIR_PROPERTY to dir.toString()) {
            val replay = ReplayRunner(config, source, object : ReplayListener {}).traceReplays({ FailOnBad() }, "Drv").single()
            assertThrows<AssertionError> { replay.run() }
        }

        assertEquals(listOf<Int?>(null), requestedMaxSteps)
        assertTrue(error.message!!.startsWith("Failure in trace 1, step 4"), error.message)
    }

    @Test
    fun `MaxStepsConfig replaces the config's own max-steps`() {
        val command = MaxStepsConfig(config.copy(maxSteps = 20), 3).toCommand(Path.of("/tmp"))

        assertEquals(1, command.count { it == "--max-steps" })
        assertEquals("3", command[command.indexOf("--max-steps") + 1])
    }
}
