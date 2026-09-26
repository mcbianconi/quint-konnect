@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// MultiActionDriver below has no `override fun step`, so every test in this class also exercises
// qk-vmya: Driver.step's default implementation reaching the generated `generatedStep` via
// reflection, not just StepMethodGenerator's own codegen.
class StepMethodGeneratorTest {

    private companion object {
        // The Kotlin *source* text for the annotation argument, escapes included, e.g.
        // `"never\$\"taken"`, mirroring the escaping fixture in
        // example/.../escaping/EscapingCounterDriver.kt (qk-gu38).
        val escapedActionSourceLiteral = "\"never\\\$\\\"taken\""

        // The decoded runtime value of that literal: `never$"taken`.
        val escapedActionRuntimeValue = "never\$\"taken"

        val result = compileWithProcessor(
            kotlinSource(
                "MultiActionDriver.kt",
                """
                package multi

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class MultiActionDriver : Driver {
                    var lastAction: String = ""
                    var lastArgs: List<Any?> = emptyList()

                    @QuintAction("simple")
                    fun simple(x: Long, flag: Boolean) {
                        lastAction = "simple"
                        lastArgs = listOf(x, flag)
                    }

                    @QuintAction("withNullable")
                    fun withNullable(maybe: Long?) {
                        lastAction = "withNullable"
                        lastArgs = listOf(maybe)
                    }

                    @QuintAction("withGeneric")
                    fun withGeneric(xs: List<Long>, m: Map<Long, String>, s: Set<String>) {
                        lastAction = "withGeneric"
                        lastArgs = listOf(xs, m, s)
                    }

                    @QuintAction
                    fun defaultNamed() {
                        lastAction = "defaultNamed"
                    }

                    @QuintAction("stepNamed")
                    fun stepNamed(step: Long) {
                        lastAction = "stepNamed"
                        lastArgs = listOf(step)
                    }

                    @QuintAction(__ESCAPED__)
                    fun escaped() {
                        lastAction = "escaped"
                    }
                }
                """.trimIndent().replace("__ESCAPED__", escapedActionSourceLiteral),
            ),
        )

        val driverClass = run {
            check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
            result.classLoader.loadClass("multi.MultiActionDriver")
        }

        fun newDriver(): Driver = driverClass.getDeclaredConstructor().newInstance() as Driver

        fun Driver.lastAction(): String = field("lastAction") as String

        @Suppress("UNCHECKED_CAST")
        fun Driver.lastArgs(): List<Any?> = field("lastArgs") as List<Any?>

        fun Any.field(name: String): Any? =
            javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)
    }

    @Test
    fun `compiles the driver and generates a Steps file`() {
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `dispatches to the annotated method with decoded primitive args`() {
        val driver = newDriver()
        driver.step(testStep("simple", mapOf("x" to ItfValue.Num(42), "flag" to ItfValue.Bool(true))))
        assertEquals("simple", driver.lastAction())
        assertEquals(listOf(42L, true), driver.lastArgs())
    }

    @Test
    fun `falls back to the function name when the annotation name is blank`() {
        val driver = newDriver()
        driver.step(testStep("defaultNamed"))
        assertEquals("defaultNamed", driver.lastAction())
    }

    @Test
    fun `decodeOrNull returns the value when the nondet pick is present`() {
        val driver = newDriver()
        driver.step(testStep("withNullable", mapOf("maybe" to ItfValue.Num(7))))
        assertEquals(listOf(7L), driver.lastArgs())
    }

    @Test
    fun `decodeOrNull returns null when the nondet pick is absent`() {
        val driver = newDriver()
        driver.step(testStep("withNullable"))
        assertNull(driver.lastArgs().single())
    }

    @Test
    fun `decode throws when a required nondet pick is missing`() {
        val driver = newDriver()
        val thrown = assertThrows(IllegalStateException::class.java) {
            driver.step(testStep("simple", mapOf("flag" to ItfValue.Bool(false))))
        }
        assertTrue(thrown.message.orEmpty().contains("x"), thrown.message)
    }

    @Test
    fun `decodes List Map and Set generic params`() {
        val driver = newDriver()
        driver.step(
            testStep(
                "withGeneric",
                mapOf(
                    "xs" to ItfValue.List(listOf(ItfValue.Num(1), ItfValue.Num(2))),
                    "m" to ItfValue.Map(listOf(ItfValue.Num(1) to ItfValue.Str("one"))),
                    "s" to ItfValue.Set(listOf(ItfValue.Str("a"), ItfValue.Str("b"))),
                ),
            ),
        )
        assertEquals(
            listOf(listOf(1L, 2L), mapOf(1L to "one"), setOf("a", "b")),
            driver.lastArgs(),
        )
    }

    @Test
    fun `a parameter named step shadows the Step argument correctly`() {
        val driver = newDriver()
        driver.step(testStep("stepNamed", mapOf("step" to ItfValue.Num(99))))
        assertEquals("stepNamed", driver.lastAction())
        assertEquals(listOf(99L), driver.lastArgs())
    }

    @Test
    fun `dispatches on an escaped action name containing dollar and quote characters`() {
        val driver = newDriver()
        driver.step(testStep(escapedActionRuntimeValue))
        assertEquals("escaped", driver.lastAction())
    }

    @Test
    fun `unknown action throws`() {
        val driver = newDriver()
        val thrown = assertThrows(IllegalStateException::class.java) {
            driver.step(testStep("nope"))
        }
        assertTrue(thrown.message.orEmpty().contains("nope"), thrown.message)
    }
}
