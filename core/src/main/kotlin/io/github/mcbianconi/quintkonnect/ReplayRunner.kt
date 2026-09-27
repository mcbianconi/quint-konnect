package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.listener.ConsoleReplayListener
import io.github.mcbianconi.quintkonnect.listener.ReplayListener
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.ItfFileTraceSource
import io.github.mcbianconi.quintkonnect.trace.TraceSource
import io.github.mcbianconi.quintkonnect.trace.defaultTraceSource
import io.github.mcbianconi.quintkonnect.trace.replayCommand
import io.github.mcbianconi.quintkonnect.trace.writeFailureTrace
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors

private val zeroTracesMessage =
    "Trace generation produced zero traces.\n" +
        "Please check your specification and/or your test configuration."

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.parallelism` Gradle property. Only affects runTest's own thread pool below;
// traceReplays' per-trace dynamic tests parallelize through JUnit's own dynamic test execution
// instead (see README.md's "Running traces in parallel" section).
internal const val PARALLELISM_PROPERTY: String = "quintkonnect.parallelism"

internal fun resolveParallelism(override: String? = System.getProperty(PARALLELISM_PROPERTY)): Int =
    override?.toIntOrNull()?.coerceAtLeast(1) ?: 1

/**
 * Generates traces from [generatorConfig] via [traceSource] and replays each one against a fresh
 * driver from a driver factory, reporting progress to [listener].
 *
 * A step or state mismatch surfaces as an [AssertionError] naming the trace, step, action and
 * nondet picks, with the original throwable as its cause; [listener] is notified with the same
 * error before it is thrown. [generatorConfig]'s reproduce seed is only reported for failures
 * during replay, not for a failure raised by [traceSource] itself.
 */
public class ReplayRunner(
    private val generatorConfig: GeneratorConfig,
    private val traceSource: TraceSource = defaultTraceSource(),
    private val listener: ReplayListener = ConsoleReplayListener(),
) {

    /**
     * Replays every trace against its own fresh driver, sequentially by default. Set the
     * `quintkonnect.parallelism` system property (the quintkonnect Gradle plugin's
     * `-Pquint.parallelism` override) above 1 to replay traces on a fixed thread pool of that size
     * instead: traces are independent (a fresh driver per trace), so this is safe as long as
     * [listener] and the driver factory are. The failure contract is unchanged either way: if any
     * trace fails, the lowest-index failure is the one thrown (and the one [listener] is notified
     * of via [ReplayListener.onRunFinished]) — in parallel, every trace still runs to completion
     * first, unlike the sequential loop, which stops at the first failure.
     */
    public fun <D : Driver> runTest(driverFactory: () -> D, testName: String) {
        listener.onRunStarted(testName, generatorConfig)
        val traces = traceSource.generate(generatorConfig)

        var failure: Throwable? = null
        try {
            check(traces.isNotEmpty()) { zeroTracesMessage }

            val parallelism = resolveParallelism()
            if (parallelism <= 1) {
                traces.forEachIndexed { traceIdx, trace ->
                    listener.onTraceStarted(traceIdx)
                    replaySteps(traceIdx, trace, driverFactory())
                    listener.onTraceFinished(traceIdx)
                }
            } else {
                runTracesInParallel(traces, driverFactory, parallelism)
            }
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            listener.onRunFinished(testName, generatorConfig, failure)
        }
    }

    private fun <D : Driver> runTracesInParallel(traces: List<ItfTrace>, driverFactory: () -> D, parallelism: Int) {
        val executor = Executors.newFixedThreadPool(parallelism)
        val failures = try {
            traces.mapIndexed { traceIdx, trace ->
                executor.submit(
                    Callable {
                        try {
                            listener.onTraceStarted(traceIdx)
                            replaySteps(traceIdx, trace, driverFactory())
                            listener.onTraceFinished(traceIdx)
                            null
                        } catch (e: Throwable) {
                            e
                        }
                    },
                )
            }.map { it.get() }
        } finally {
            executor.shutdown()
        }
        failures.firstOrNull { it != null }?.let { throw it }
    }

    /**
     * Generates traces from [generatorConfig] once, then returns one [TraceReplay] per trace: its
     * [TraceReplay.run] creates a fresh driver via [driverFactory] and replays only that trace, so
     * a runner-neutral caller (e.g. a JUnit `@TestFactory`) can report and run each trace on its
     * own. A failing trace calls [ReplayListener.onTraceFailed], then [ReplayListener.onTraceFailureSaved]
     * once the trace is saved as ITF JSON (or an attempt was made to); other traces are unaffected.
     *
     * Zero traces still fails visibly: the returned list has a single [TraceReplay] whose [run]
     * throws [IllegalStateException].
     */
    public fun <D : Driver> traceReplays(driverFactory: () -> D, testName: String): List<TraceReplay> {
        listener.onRunStarted(testName, generatorConfig)
        val traces = traceSource.generate(generatorConfig)

        if (traces.isEmpty()) {
            return listOf(
                TraceReplay("no traces generated") {
                    val failure = IllegalStateException(zeroTracesMessage)
                    listener.onRunFinished(testName, generatorConfig, failure)
                    throw failure
                },
            )
        }

        return traces.mapIndexed { traceIdx, trace ->
            TraceReplay("trace ${traceIdx + 1} (seed ${generatorConfig.seed})") {
                listener.onTraceStarted(traceIdx)
                try {
                    replaySteps(traceIdx, trace, driverFactory())
                    listener.onTraceFinished(traceIdx)
                } catch (e: Throwable) {
                    listener.onTraceFailed(traceIdx, generatorConfig, e)
                    val (failureFile, command) = saveFailureTrace(testName, traceIdx, trace, e)
                    listener.onTraceFailureSaved(traceIdx, testName, failureFile, command)
                    throw e
                }
            }
        }
    }

    // Skips writing (and points the printed command at the input instead) when this run was
    // itself replaying a saved trace: re-saving under the same default naming scheme could
    // overwrite an unrelated earlier failure that happens to share a testName/trace index.
    private fun saveFailureTrace(testName: String, traceIdx: Int, trace: ItfTrace, originalFailure: Throwable): Pair<Path?, String?> {
        val replayedFrom = (traceSource as? ItfFileTraceSource)?.path
        if (replayedFrom != null) {
            return replayedFrom to replayCommand(testName, replayedFrom)
        }
        return try {
            val file = writeFailureTrace(testName, traceIdx, trace)
            file to replayCommand(testName, file)
        } catch (writeError: IOException) {
            originalFailure.addSuppressed(writeError)
            null to null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <D : Driver> replaySteps(traceIdx: Int, trace: ItfTrace, driver: D) {
        val state = driver.quintState() as State<D>

        trace.states.forEachIndexed { stepIdx, itfState ->
            var step: Step? = null
            try {
                listener.onStepStarted(traceIdx, stepIdx, itfState)
                step = Step.fromState(itfState.value, driver.config())
                listener.onStep(traceIdx, stepIdx, step)

                check(step.actionTaken.isNotEmpty()) {
                    "An anonymous action was found!\n" +
                        "Please make sure all actions in the specification are properly named."
                }

                driver.step(step)
                state.check(driver, step.state)
            } catch (e: Throwable) { // Throwable, not Exception: JUnit/kotlin.test failures are AssertionError
                val location = "trace ${traceIdx + 1}, step $stepIdx" +
                    (step?.let { ", action '${it.actionTaken}'" } ?: "")
                val nondets = step?.nondetPicks?.takeIf { !it.isEmpty() }
                    ?.let { "\nNondet picks:\n$it" } ?: ""
                val wrapped = AssertionError("Failure in $location$nondets\n${e.message ?: e}", e)
                listener.onStepFailed(traceIdx, stepIdx, step, wrapped)
                throw wrapped
            }
        }
    }
}
