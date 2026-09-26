package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfTrace
import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.display
import io.github.mcbianconi.quintkonnect.logger.Logger
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.TraceGenerator

object Runner {

    fun <D : Driver> runTest(
        driverFactory: () -> D,
        generatorConfig: GeneratorConfig,
        testName: String,
    ) {
        Logger.title("Running model based tests for $testName")
        Logger.info(
            "Generating ${generatorConfig.nTraces} traces using " +
                "`${generatorConfig.seed}` as random seed ..."
        )

        val traces = TraceGenerator.generate(generatorConfig)

        runTest(driverFactory, generatorConfig, testName, traces)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <D : Driver> runTest(
        driverFactory: () -> D,
        generatorConfig: GeneratorConfig,
        testName: String,
        traces: List<ItfTrace>,
    ) {
        try {
            check(traces.isNotEmpty()) {
                "Trace generation produced zero traces.\n" +
                    "Please check your specification and/or your test configuration."
            }

            Logger.info("Replaying traces ...")

            traces.forEachIndexed { traceIdx, trace ->
                Logger.trace(1, "[Trace ${traceIdx + 1}]")
                val driver = driverFactory()
                val state = driver.quintState() as State<D>

                trace.states.forEachIndexed { stepIdx, itfState ->
                    var step: Step? = null
                    try {
                        Logger.trace(2, "Deriving step from:\n${ItfValue.Record(itfState.value).display()}\n")

                        step = Step.fromState(itfState.value, driver.config())
                        Logger.trace(1, "[Step $stepIdx]\n$step\n")

                        check(step.actionTaken.isNotEmpty()) {
                            "An anonymous action was found!\n" +
                                "Please make sure all actions in the specification are properly named."
                        }

                        driver.step(step)

                        Logger.trace(2, "Extracting state from:\n${step.state.display()}\n")
                        state.check(driver, step.state)
                    } catch (e: Throwable) { // Throwable, not Exception: JUnit/kotlin.test failures are AssertionError
                        val location = "trace ${traceIdx + 1}, step $stepIdx" +
                            (step?.let { ", action '${it.actionTaken}'" } ?: "")
                        val nondets = step?.nondetPicks?.takeIf { !it.isEmpty() }
                            ?.let { "\nNondet picks:\n$it" } ?: ""
                        throw AssertionError("Failure in $location$nondets\n${e.message ?: e}", e)
                    }
                }
            }

            Logger.success("[OK] $testName")
        } catch (e: Throwable) {
            Logger.error("[FAIL] $testName")
            Logger.error("Reproduce this error with `QUINT_SEED=${generatorConfig.seed}`\n")
            throw e
        }
    }
}
