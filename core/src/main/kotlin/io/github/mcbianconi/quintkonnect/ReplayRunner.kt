package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.listener.ConsoleReplayListener
import io.github.mcbianconi.quintkonnect.listener.ReplayListener
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.TraceGenerator
import io.github.mcbianconi.quintkonnect.trace.TraceSource

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
    private val traceSource: TraceSource = TraceGenerator,
    private val listener: ReplayListener = ConsoleReplayListener(),
) {

    @Suppress("UNCHECKED_CAST")
    public fun <D : Driver> runTest(driverFactory: () -> D, testName: String) {
        listener.onRunStarted(testName, generatorConfig)
        val traces = traceSource.generate(generatorConfig)

        var failure: Throwable? = null
        try {
            check(traces.isNotEmpty()) {
                "Trace generation produced zero traces.\n" +
                    "Please check your specification and/or your test configuration."
            }

            traces.forEachIndexed { traceIdx, trace ->
                listener.onTraceStarted(traceIdx)
                val driver = driverFactory()
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
                listener.onTraceFinished(traceIdx)
            }
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            listener.onRunFinished(testName, generatorConfig, failure)
        }
    }
}
