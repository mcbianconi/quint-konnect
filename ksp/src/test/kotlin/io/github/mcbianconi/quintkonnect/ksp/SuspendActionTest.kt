@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// qk-33ky: generatedStep stays non-suspend (Driver.step's reflective default dispatch calls it as
// a plain function); a suspend @QuintAction's call is wrapped in kotlinx.coroutines.runBlocking.
class SuspendActionTest {

    private companion object {
        val result = compileWithProcessor(
            kotlinSource(
                "SuspendDriver.kt",
                """
                package suspendaction

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun
                import kotlinx.coroutines.delay

                @QuintRun(spec = "unused.qnt")
                class SuspendDriver : Driver {
                    var lastAction: String = ""

                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("suspending")
                    suspend fun suspending(x: Long) {
                        delay(1)
                        lastAction = "suspending:${'$'}x"
                    }

                    @QuintAction("plain")
                    fun plain() {
                        lastAction = "plain"
                    }
                }
                """.trimIndent(),
            ),
        )

        val driverClass = run {
            check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
            result.classLoader.loadClass("suspendaction.SuspendDriver")
        }

        fun newDriver(): Driver = driverClass.getDeclaredConstructor().newInstance() as Driver

        fun Driver.lastAction(): String =
            javaClass.getDeclaredField("lastAction").apply { isAccessible = true }.get(this) as String
    }

    @Test
    fun `compiles a driver with a suspend QuintAction`() {
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `dispatches to a suspend action via runBlocking`() {
        val driver = newDriver()
        driver.step(testStep("suspending", mapOf("x" to ItfValue.Num(5))))
        assertEquals("suspending:5", driver.lastAction())
    }

    @Test
    fun `dispatches to a plain action alongside a suspend one in the same driver`() {
        val driver = newDriver()
        driver.step(testStep("plain"))
        assertEquals("plain", driver.lastAction())
    }
}
