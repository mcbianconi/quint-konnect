@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path

// Kept as a literal: quintkonnect.replay is `internal` to :core (REPLAY_PROPERTY in
// trace/TraceSource.kt).
private const val REPLAY_PROPERTY = "quintkonnect.replay"

private fun withSystemProperty(name: String, value: String, block: () -> Unit) {
    val previous = System.getProperty(name)
    System.setProperty(name, value)
    try {
        block()
    } finally {
        if (previous == null) System.clearProperty(name) else System.setProperty(name, previous)
    }
}

// One state per trace is enough: DriverConfig()'s default path expects only `mbt::actionTaken`/
// `mbt::nondetPicks`, and ConcurrencyDriver.step (below) ignores the rest of the state entirely.
private fun writeTraceFixtures(dir: Path, count: Int) {
    repeat(count) { i ->
        dir.resolve("trace${i + 1}.itf.json").toFile().writeText(
            """{"vars":["mbt::actionTaken","mbt::nondetPicks"],""" +
                """"states":[{"mbt::actionTaken":"act","mbt::nondetPicks":{}}]}""",
        )
    }
}

// Proves qk-voya's fix: a generated `@TestFactory fun traces()` carries
// `@Execution(ExecutionMode.CONCURRENT)` (QuintRunTestGenerator/QuintTestTestGenerator), and dynamic
// tests it produces inherit that mode from their factory method when JUnit's own parallel execution
// is enabled — verified here by running the *actual* generated class through the JUnit Platform
// Launcher (not just invoking `traces()` by reflection, which never touches JUnit's executor).
class ParallelDynamicTestExecutionTest {

    private companion object {
        const val TRACE_COUNT = 4
        const val SLEEP_MILLIS = 300L

        val result = compileWithProcessor(
            kotlinSource(
                "ConcurrencyDriver.kt",
                """
                package concurrency

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import io.github.mcbianconi.quintkonnect.ksp.ConcurrencyRecorder

                @QuintRun(spec = "unused.qnt")
                class ConcurrencyDriver : Driver {
                    override fun step(step: Step) {
                        val start = System.nanoTime()
                        Thread.sleep(${SLEEP_MILLIS}L)
                        ConcurrencyRecorder.record(start, System.nanoTime(), Thread.currentThread().name)
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `dynamic trace tests overlap in time when JUnit parallel execution is enabled`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
        ConcurrencyRecorder.reset()

        val traceDir = Files.createTempDirectory("quint-parallel-proof")
        writeTraceFixtures(traceDir, TRACE_COUNT)
        val testClass = result.classLoader.loadClass("concurrency.ConcurrencyDriverQuintRunTest")

        withSystemProperty(REPLAY_PROPERTY, traceDir.toString()) {
            runViaLauncher(testClass, parallelEnabled = true)
        }

        val recorded = ConcurrencyRecorder.recorded()
        assertEquals(TRACE_COUNT, recorded.size)
        assertTrue(ConcurrencyRecorder.anyOverlap()) {
            "Expected at least two traces to overlap in time, got: $recorded"
        }
        assertTrue(recorded.map { it.threadName }.toSet().size > 1) {
            "Expected traces to run on more than one thread, got: $recorded"
        }
    }

    @Test
    fun `dynamic trace tests stay sequential when JUnit parallel execution is not enabled`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
        ConcurrencyRecorder.reset()

        val traceDir = Files.createTempDirectory("quint-sequential-proof")
        writeTraceFixtures(traceDir, TRACE_COUNT)
        val testClass = result.classLoader.loadClass("concurrency.ConcurrencyDriverQuintRunTest")

        withSystemProperty(REPLAY_PROPERTY, traceDir.toString()) {
            runViaLauncher(testClass, parallelEnabled = false)
        }

        val recorded = ConcurrencyRecorder.recorded()
        assertEquals(TRACE_COUNT, recorded.size)
        assertTrue(!ConcurrencyRecorder.anyOverlap()) {
            "Expected no overlap when parallel execution is disabled (the @Execution annotation " +
                "should be a no-op), got: $recorded"
        }
    }

    private fun runViaLauncher(testClass: Class<*>, parallelEnabled: Boolean) {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(testClass))
            .configurationParameter("junit.jupiter.execution.parallel.enabled", parallelEnabled.toString())
            .configurationParameter("junit.jupiter.execution.parallel.mode.default", "same_thread")
            .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "same_thread")
            .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
            .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", TRACE_COUNT.toString())
            .build()

        val listener = SummaryGeneratingListener()
        LauncherFactory.create().execute(request, listener)

        val summary = listener.summary
        val failures = StringWriter()
        summary.printFailuresTo(PrintWriter(failures))
        check(summary.testsFailedCount == 0L) { "Expected no failures, got:\n$failures" }
    }
}
