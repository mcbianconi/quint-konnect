@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParameterShapesTest {

    private companion object {
        val result = compileWithProcessor(
            kotlinSource(
                "ParameterShapesDriver.kt",
                """
                package shapes

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                typealias UserId = Long

                @JvmInline
                value class Flag(val value: Boolean)

                class Container {
                    class Nested(val n: Long)
                }

                @QuintRun(spec = "unused.qnt")
                class ParameterShapesDriver : Driver {
                    var lastAction: String = ""
                    var lastArgs: List<Any?> = emptyList()

                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("withTypeAlias")
                    fun withTypeAlias(id: UserId) {
                        lastAction = "withTypeAlias"
                        lastArgs = listOf(id)
                    }

                    @QuintAction("withOutVariance")
                    fun withOutVariance(xs: List<out Long>) {
                        lastAction = "withOutVariance"
                        lastArgs = listOf(xs)
                    }

                    @QuintAction("withValueClass")
                    fun withValueClass(flag: Flag) {
                        lastAction = "withValueClass"
                    }

                    @QuintAction("withNestedClass")
                    fun withNestedClass(nested: Container.Nested) {
                        lastAction = "withNestedClass"
                    }
                }
                """.trimIndent(),
            ),
        )

        val driverClass = run {
            check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
            result.classLoader.loadClass("shapes.ParameterShapesDriver")
        }

        fun newDriver(): Driver = driverClass.getDeclaredConstructor().newInstance() as Driver

        fun Driver.lastAction(): String = field("lastAction") as String

        @Suppress("UNCHECKED_CAST")
        fun Driver.lastArgs(): List<Any?> = field("lastArgs") as List<Any?>

        fun Any.field(name: String): Any? =
            javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)
    }

    @Test
    fun `compiles a driver with a typealias, a value class, a nested class and an out-projected List parameter`() {
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `decodes a parameter whose type is a typealias`() {
        val driver = newDriver()
        driver.step(testStep("withTypeAlias", mapOf("id" to ItfValue.Num(7))))
        assertEquals("withTypeAlias", driver.lastAction())
        assertEquals(listOf(7L), driver.lastArgs())
    }

    @Test
    fun `decodes a List parameter with an out-projected type argument`() {
        val driver = newDriver()
        driver.step(
            testStep(
                "withOutVariance",
                mapOf("xs" to ItfValue.List(listOf(ItfValue.Num(1), ItfValue.Num(2)))),
            ),
        )
        assertEquals("withOutVariance", driver.lastAction())
        assertEquals(listOf(listOf(1L, 2L)), driver.lastArgs())
    }

    // Decoding a value class or a user-defined nested class into a real value needs the
    // kotlinx-serialization compiler plugin (to generate their `serializer()`), which this ad hoc
    // compilation doesn't enable. Omitting the pick still proves the branch dispatches and reaches
    // `decode`, since `decode` checks for the pick before resolving a serializer for its type.
    @Test
    fun `dispatches to the branch for a value class parameter before decoding it`() {
        val driver = newDriver()
        val thrown = assertThrows(IllegalStateException::class.java) {
            driver.step(testStep("withValueClass"))
        }
        assertTrue(thrown.message.orEmpty().contains("flag"), thrown.message)
    }

    @Test
    fun `dispatches to the branch for a nested class parameter before decoding it`() {
        val driver = newDriver()
        val thrown = assertThrows(IllegalStateException::class.java) {
            driver.step(testStep("withNestedClass"))
        }
        assertTrue(thrown.message.orEmpty().contains("nested"), thrown.message)
    }
}
