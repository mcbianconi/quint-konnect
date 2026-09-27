package io.github.mcbianconi.quintkonnect.listener

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.display
import io.github.mcbianconi.quintkonnect.Step
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import java.io.PrintStream
import java.nio.file.Path

// Set by the quintkonnect Gradle plugin's Test tasks (QuintKonnectPlugin.kt) from the
// `-Pquint.verbose` Gradle property, for PR vs nightly CI profiles. Override > QUINT_VERBOSE > 0.
internal const val VERBOSE_PROPERTY: String = "quintkonnect.verbose"

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
 * Whether ANSI colour codes should be emitted.
 *
 * `quintColor` (`QUINT_COLOR` env var) is an override: `"always"`/`"never"` decide it outright,
 * any other value (including unset) falls through to `hasConsole && noColor.isNullOrEmpty()`.
 * `noColor` (`NO_COLOR` env var) disables colour when present and not an empty string, regardless
 * of its value, per https://no-color.org. `hasConsole` is `System.console() != null`, false
 * whenever output isn't a terminal (e.g. piped, or a CI log).
 */
internal fun resolveUseColor(noColor: String?, quintColor: String?, hasConsole: Boolean): Boolean =
    when (quintColor) {
        "always" -> true
        "never" -> false
        else -> hasConsole && noColor.isNullOrEmpty()
    }

private fun defaultUseColor(): Boolean =
    resolveUseColor(System.getenv("NO_COLOR"), System.getenv("QUINT_COLOR"), System.console() != null)

/**
 * Verbosity for the no-arg [ConsoleReplayListener] constructor: `verboseProperty`
 * (`quintkonnect.verbose`, the quintkonnect Gradle plugin's `-Pquint.verbose` override) wins when
 * set and numeric, then `quintVerbose` (`QUINT_VERBOSE` env var), then `0`.
 */
internal fun resolveVerbosity(verboseProperty: String?, quintVerbose: String?): Int =
    verboseProperty?.toIntOrNull() ?: quintVerbose?.toIntOrNull() ?: 0

/**
 * The default [ReplayListener]: prints replay progress to [err] at [verbosity].
 *
 * The no-arg constructor honors the `quintkonnect.verbose` system property (falling back to
 * `QUINT_VERBOSE`) and prints to [System.err], read when this listener is constructed, not at
 * class init. Every public constructor also decides ANSI colour
 * the same way: `QUINT_COLOR=always`/`QUINT_COLOR=never` force it on/off, `NO_COLOR`
 * (https://no-color.org) disables it when set to a non-empty value, and otherwise it's on only
 * when [System.console] is non-null (a real terminal).
 */
public class ConsoleReplayListener(
    private val verbosity: Int,
    private val err: PrintStream,
) : ReplayListener {

    private var useColor: Boolean = defaultUseColor()

    internal constructor(verbosity: Int, err: PrintStream, useColor: Boolean) : this(verbosity, err) {
        this.useColor = useColor
    }

    public constructor() : this(
        resolveVerbosity(System.getProperty(VERBOSE_PROPERTY), System.getenv("QUINT_VERBOSE")),
        System.err,
    )

    private fun wrap(vararg codes: String, text: String): String =
        if (useColor) "${codes.joinToString("")}$text$RESET" else text

    private fun title(msg: String) = err.println(wrap(BOLD, text = "== $msg"))

    private fun info(msg: String) = err.println(indent(msg))

    private fun success(msg: String) = err.println(wrap(BOLD, GREEN, text = indent(msg)))

    private fun error(msg: String) = err.println(wrap(BOLD, RED, text = indent(msg)))

    private fun trace(level: Int, msg: String) {
        if (verbosity >= level) {
            err.println(wrap(DIM, WHITE, text = indent(msg)))
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

    override fun onTraceFailed(traceIndex: Int, config: GeneratorConfig, failure: Throwable) {
        error("[FAIL] trace ${traceIndex + 1}")
        error("Reproduce this error with `QUINT_SEED=${config.seed}`\n")
    }

    override fun onTraceFailureSaved(traceIndex: Int, testName: String, failureFile: Path?, replayCommand: String?) {
        if (failureFile != null && replayCommand != null) {
            info("Saved failing trace to $failureFile")
            info("Replay it with:\n   $replayCommand\n")
        }
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
