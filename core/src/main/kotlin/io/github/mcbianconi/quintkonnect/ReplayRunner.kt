package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.listener.ConsoleReplayListener
import io.github.mcbianconi.quintkonnect.listener.ReplayListener
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.ItfFileTraceSource
import io.github.mcbianconi.quintkonnect.trace.MaxStepsConfig
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import io.github.mcbianconi.quintkonnect.trace.TraceSource
import io.github.mcbianconi.quintkonnect.trace.TracesDirTraceSource
import io.github.mcbianconi.quintkonnect.trace.defaultTraceSource
import io.github.mcbianconi.quintkonnect.trace.replayCommand
import io.github.mcbianconi.quintkonnect.trace.shrinkEnabled
import io.github.mcbianconi.quintkonnect.trace.writeFailureTrace
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val zeroTracesMessage =
    "Trace generation produced zero traces.\n" +
        "Please check your specification and/or your test configuration."

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.parallelism` Gradle property. Only affects runTest's own thread pool below;
// traceReplays' per-trace dynamic tests parallelize through JUnit's own dynamic test execution
// instead (see README.md's "Run traces in parallel" section).
internal const val PARALLELISM_PROPERTY: String = "quintkonnect.parallelism"

internal fun resolveParallelism(override: String? = System.getProperty(PARALLELISM_PROPERTY)): Int =
    override?.toIntOrNull()?.coerceAtLeast(1) ?: 1

// ReplayRunner's traceSource constructor parameter defaults to this sentinel, not directly to
// defaultTraceSource(testName) (trace/TraceSource.kt): testName isn't known until runTest/
// traceReplays is called, one step after construction. Never call generate() on this directly.
private object DefaultTraceSource : TraceSource {
    override fun generate(config: GeneratorConfig): List<ItfTrace> =
        error("DefaultTraceSource.generate() called directly; ReplayRunner should have resolved it via resolveTraceSource() first.")
}

// generatorConfig.seed as baked into the generated RunConfig/TestConfig at test time isn't
// necessarily the seed generateQuintTraces (gradle-plugin, qk-adm2) actually ran `quint` with when
// replaying from a TracesDirTraceSource; this substitutes the recorded one for display purposes
// only (ReplayRunner never calls generate() on it, so it never affects which trace is replayed).
// The AssertionError replaySteps throws, carrying the failing step for shrinking (qk-a8ay).
internal class StepFailure(message: String, cause: Throwable, val stepIdx: Int) : AssertionError(message, cause)

private object SilentListener : ReplayListener

private class SeedOverriddenConfig(
    private val delegate: GeneratorConfig,
    override val seed: String,
) : GeneratorConfig by delegate

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
    private val traceSource: TraceSource = DefaultTraceSource,
    private val listener: ReplayListener = ConsoleReplayListener(),
) {

    // [testName] isn't known until runTest/traceReplays is called, one step after this class is
    // constructed (traceSource's default value above is evaluated at construction time), so
    // resolving DefaultTraceSource into the real testName-aware default (defaultTraceSource(),
    // trace/TraceSource.kt) has to happen here instead, the first time it's actually needed.
    // [traceSource] itself is left untouched either way (=== DefaultTraceSource is what
    // saveFailureTrace's `as? ItfFileTraceSource` check below relies on to skip re-saving a replayed
    // failure), and an explicitly injected [traceSource] is never second-guessed against tracesDir.
    private val shrinkAttempted = AtomicBoolean(false)

    private fun resolveTraceSource(testName: String): Pair<TraceSource, GeneratorConfig> {
        if (traceSource !== DefaultTraceSource) return traceSource to generatorConfig
        val resolved = defaultTraceSource(testName)
        val recordedSeed = (resolved as? TracesDirTraceSource)?.recordedSeed()
        val displayConfig = recordedSeed?.let { SeedOverriddenConfig(generatorConfig, it) } ?: generatorConfig
        return resolved to displayConfig
    }

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
        val (resolvedSource, displayConfig) = resolveTraceSource(testName)
        listener.onRunStarted(testName, displayConfig)
        val traces = resolvedSource.generate(generatorConfig)

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
            listener.onRunFinished(testName, displayConfig, failure)
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
        val (resolvedSource, displayConfig) = resolveTraceSource(testName)
        listener.onRunStarted(testName, displayConfig)
        val traces = resolvedSource.generate(generatorConfig)

        if (traces.isEmpty()) {
            return listOf(
                TraceReplay("no traces generated") {
                    val failure = IllegalStateException(zeroTracesMessage)
                    listener.onRunFinished(testName, displayConfig, failure)
                    throw failure
                },
            )
        }

        return traces.mapIndexed { traceIdx, trace ->
            TraceReplay("trace ${traceIdx + 1} (seed ${displayConfig.seed})") {
                listener.onTraceStarted(traceIdx)
                try {
                    replaySteps(traceIdx, trace, driverFactory())
                    listener.onTraceFinished(traceIdx)
                } catch (e: Throwable) {
                    val shrunk = shrinkIfEnabled(e, resolvedSource, driverFactory)
                    val reported = shrunk?.second ?: e
                    listener.onTraceFailed(traceIdx, displayConfig, reported)
                    val (failureFile, command) = if (shrunk == null) {
                        saveFailureTrace(testName, traceIdx, trace, e, resolvedSource)
                    } else {
                        saveFailureTrace(testName, shrunk.third, shrunk.first, reported, resolvedSource, "$testName-shrunk")
                    }
                    listener.onTraceFailureSaved(traceIdx, testName, failureFile, command)
                    throw reported
                }
            }
        }
    }

    // qk-a8ay: with shrinking on (the plugin's shrinkQuintTraces task), the first failing trace of a
    // `quint run` config is regenerated with the same seed at --max-steps 0, 1, ... up to one below
    // its failing step; the first run with a failing trace gives the shortest failure found, as
    // (trace, error, its index). Null when shrinking is off, doesn't apply (replaying saved traces,
    // `quint test`, an error that isn't a step failure) or finds nothing shorter.
    private fun <D : Driver> shrinkIfEnabled(
        failure: Throwable,
        source: TraceSource,
        driverFactory: () -> D,
    ): Triple<ItfTrace, AssertionError, Int>? {
        if (!shrinkEnabled() || failure !is StepFailure || generatorConfig !is RunConfig) return null
        if (source is ItfFileTraceSource || source is TracesDirTraceSource) return null
        if (!shrinkAttempted.compareAndSet(false, true)) return null

        for (maxSteps in 0 until failure.stepIdx) {
            val traces = try {
                source.generate(MaxStepsConfig(generatorConfig, maxSteps))
            } catch (e: Exception) {
                failure.addSuppressed(e)
                return null
            }
            traces.forEachIndexed { idx, candidate ->
                try {
                    replaySteps(idx, candidate, driverFactory(), SilentListener)
                } catch (shorter: StepFailure) {
                    val message = "Shrunk failing trace: it failed at step ${failure.stepIdx}; rerunning " +
                        "quint with the same seed (${generatorConfig.seed}) and --max-steps $maxSteps, " +
                        "trace ${idx + 1} fails at step ${shorter.stepIdx}.\n${shorter.message}"
                    return Triple(candidate, AssertionError(message, shorter), idx)
                }
            }
        }
        return null
    }

    // Skips writing (and points the printed command at the input instead) when this run was
    // itself replaying a saved trace: re-saving under the same default naming scheme could
    // overwrite an unrelated earlier failure that happens to share a testName/trace index.
    private fun saveFailureTrace(
        testName: String,
        traceIdx: Int,
        trace: ItfTrace,
        originalFailure: Throwable,
        resolvedSource: TraceSource,
        fileStem: String = testName,
    ): Pair<Path?, String?> {
        val replayedFrom = (resolvedSource as? ItfFileTraceSource)?.path
        if (replayedFrom != null) {
            return replayedFrom to replayCommand(testName, replayedFrom)
        }
        return try {
            val file = writeFailureTrace(fileStem, traceIdx, trace)
            file to replayCommand(testName, file)
        } catch (writeError: IOException) {
            originalFailure.addSuppressed(writeError)
            null to null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <D : Driver> replaySteps(
        traceIdx: Int,
        trace: ItfTrace,
        driver: D,
        listener: ReplayListener = this.listener,
    ) {
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
                val wrapped = StepFailure("Failure in $location$nondets\n${e.message ?: e}", e, stepIdx)
                listener.onStepFailed(traceIdx, stepIdx, step, wrapped)
                throw wrapped
            }
        }
    }
}
