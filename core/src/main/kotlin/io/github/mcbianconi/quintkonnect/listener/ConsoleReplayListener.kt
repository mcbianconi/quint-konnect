package io.github.mcbianconi.quintkonnect.listener

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.display
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import java.io.PrintStream

private const val BOLD = "\u001B[1m"
private const val GREEN = "\u001B[32m"
private const val RED = "\u001B[31m"
private const val DIM = "\u001B[2m"
private const val WHITE = "\u001B[97m"
private const val RESET = "\u001B[0m"

private fun indent(text: String, spaces: Int = 3): String {
    val prefix = " ".repeat(spaces)
    return text.lines().joinToString("\n") { prefix + it }
}

/**
 * The default [ReplayListener]: prints replay progress to [err] at [verbosity].
 *
 * The no-arg constructor honors `QUINT_VERBOSE` and prints to [System.err], read when this
 * listener is constructed, not at class init.
 */
public class ConsoleReplayListener(
    private val verbosity: Int,
    private val err: PrintStream,
) : ReplayListener {

    public constructor() : this(System.getenv("QUINT_VERBOSE")?.toIntOrNull() ?: 0, System.err)

    private fun title(msg: String) = err.println("$BOLD== $msg$RESET")

    private fun info(msg: String) = err.println(indent(msg))

    private fun success(msg: String) = err.println("$BOLD$GREEN${indent(msg)}$RESET")

    private fun error(msg: String) = err.println("$BOLD$RED${indent(msg)}$RESET")

    private fun trace(level: Int, msg: String) {
        if (verbosity >= level) {
            err.println("$DIM$WHITE${indent(msg)}$RESET")
        }
    }

    override fun onRunStarted(testName: String, config: GeneratorConfig) {
        title("Running model based tests for $testName")
        info("Generating ${config.nTraces} traces using `${config.seed}` as random seed ...")
    }

    override fun onTraceStarted(traceIndex: Int) {
        trace(1, "[Trace ${traceIndex + 1}]")
    }

    override fun onStepStarted(traceIndex: Int, stepIndex: Int, rawState: ItfState) {
        trace(2, "Deriving step from:\n${ItfValue.Record(rawState.value).display()}\n")
    }

    override fun onStep(traceIndex: Int, stepIndex: Int, step: Step) {
        trace(1, "[Step $stepIndex]\n$step\n")
    }

    override fun onRunFinished(testName: String, config: GeneratorConfig, failure: Throwable?) {
        if (failure == null) {
            success("[OK] $testName")
        } else {
            error("[FAIL] $testName")
            error("Reproduce this error with `QUINT_SEED=${config.seed}`\n")
        }
    }
}
