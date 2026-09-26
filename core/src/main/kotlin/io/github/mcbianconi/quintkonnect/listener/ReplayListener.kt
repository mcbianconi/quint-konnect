package io.github.mcbianconi.quintkonnect.listener

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig

/**
 * Observes a replay run: trace generation, each trace and step, and the outcome.
 *
 * Every method has a no-op default, so implementations only override the events they need.
 * [ConsoleReplayListener] is the default, printing progress to stderr; other implementations
 * can collect events for parallel or non-interactive runs instead.
 *
 * Trace and step indices are 0-based.
 */
public interface ReplayListener {

    /** A run for [testName] started; [config] will generate the traces it replays. */
    public fun onRunStarted(testName: String, config: GeneratorConfig) {}

    /** Replay of trace [traceIndex] started. */
    public fun onTraceStarted(traceIndex: Int) {}

    /** [rawState] at [stepIndex] of trace [traceIndex] is about to be parsed into a [Step]. */
    public fun onStepStarted(traceIndex: Int, stepIndex: Int, rawState: ItfState) {}

    /** [step] was derived from trace [traceIndex] at [stepIndex], before dispatch to the driver. */
    public fun onStep(traceIndex: Int, stepIndex: Int, step: Step) {}

    /**
     * Step extraction, driver dispatch or state comparison failed at [stepIndex] of trace
     * [traceIndex]. [step] is `null` when the failure happened during extraction itself, before a
     * [Step] existed. [failure] is the [AssertionError] the run fails with: its message names the
     * trace, step, action and nondet picks, and its cause is the original throwable.
     */
    public fun onStepFailed(traceIndex: Int, stepIndex: Int, step: Step?, failure: Throwable) {}

    /** Trace [traceIndex] replayed to completion without failure. */
    public fun onTraceFinished(traceIndex: Int) {}

    /** The run for [testName] finished; [failure] is `null` on success. */
    public fun onRunFinished(testName: String, config: GeneratorConfig, failure: Throwable?) {}
}
