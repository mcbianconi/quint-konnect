package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.dispatch.DispatchFixtureDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// qk-vmya: `Driver.step`'s default implementation dispatches to the KSP-generated
// `generatedStep(driver, step)` by looking up `<package>.<DriverClass>StepsKt` reflectively.
class GeneratedStepDispatchTest {

    private fun testStep(actionTaken: String): Step {
        val state = linkedMapOf<String, ItfValue>(
            "mbt::actionTaken" to ItfValue.Str(actionTaken),
            "mbt::nondetPicks" to ItfValue.Record(LinkedHashMap()),
        )
        return Step.fromState(state, DriverConfig())
    }

    @Test
    fun `default step dispatches to the generated function found by class name`() {
        val driver = DispatchFixtureDriver()
        driver.step(testStep("foo"))
        assertEquals("foo", driver.lastAction)
    }

    @Test
    fun `default step surfaces the action's own exception, not a reflection wrapper`() {
        val driver = DispatchFixtureDriver()
        val thrown = assertThrows<IllegalStateException> { driver.step(testStep("nope")) }
        assertTrue(thrown.message.orEmpty().contains("nope"), thrown.message)
    }

    @Test
    fun `default step throws a clear error naming the expected generated class when it's missing`() {
        val driver = MissingDispatcherDriver()
        val thrown = assertThrows<IllegalStateException> { driver.step(testStep("foo")) }
        assertTrue(
            thrown.message.orEmpty().contains("MissingDispatcherDriverStepsKt"),
            thrown.message,
        )
    }
}

private class MissingDispatcherDriver : Driver
