@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessorErrorTest {

    // qk-nqry: two @QuintAction methods sharing an action name must be a compile error.
    @Test
    fun `duplicate action names should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "DuplicateActionDriver.kt",
                """
                package dup

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class DuplicateActionDriver : Driver {
                    var lastAction = ""

                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("dup")
                    fun first() {
                        lastAction = "first"
                    }

                    @QuintAction("dup")
                    fun second() {
                        lastAction = "second"
                    }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("first"), result.messages)
        assertTrue(result.messages.contains("second"), result.messages)
    }

    // qk-9lsz: two @QuintAction methods sharing an action name must be rejected even when one is
    // inherited unchanged from an abstract base rather than declared on the driver itself.
    @Test
    fun `inherited duplicate action names should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InheritedDuplicateActionDriver.kt",
                """
                package dupinherit

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                abstract class BaseDriver : Driver {
                    @QuintAction("dup")
                    fun first() {}
                }

                @QuintRun(spec = "unused.qnt")
                class InheritedDuplicateActionDriver : BaseDriver() {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("dup")
                    fun second() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("first"), result.messages)
        assertTrue(result.messages.contains("second"), result.messages)
    }

    // qk-9lsz: a @QuintAction function must be public, since the generated dispatcher calls it
    // from a separate file.
    @Test
    fun `internal QuintAction function should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InternalActionDriver.kt",
                """
                package internalaction

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class InternalActionDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("hidden")
                    internal fun hidden() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("hidden"), result.messages)
    }

    @Test
    fun `private QuintAction function should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "PrivateActionDriver.kt",
                """
                package privateaction

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class PrivateActionDriver : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("secret")
                    private fun secret() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("secret"), result.messages)
    }

    // qk-9lsz: `Runner.runTest`'s generated `driverFactory = { X() }` call needs a public no-arg
    // constructor; report that at KSP time rather than as a confusing error in generated code.
    @Test
    fun `driver with a private constructor should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "PrivateConstructorDriver.kt",
                """
                package privatector

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class PrivateConstructorDriver private constructor() : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("PrivateConstructorDriver"), result.messages)
    }

    @Test
    fun `driver with a required constructor argument should be rejected by the processor`() {
        val result = compileWithProcessor(
            kotlinSource(
                "RequiredArgDriver.kt",
                """
                package requiredarg

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class RequiredArgDriver(val seed: Long) : Driver {
                    override fun step(step: Step) = generatedStep(step)

                    @QuintAction("go")
                    fun go() {}
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("RequiredArgDriver"), result.messages)
    }

    // qk-9lsz: a driver that inherits @QuintAction functions from an abstract base (some
    // untouched, one overridden without re-annotating) must compile and dispatch correctly.
    @Test
    fun `driver inheriting actions from an abstract base compiles and dispatches`() {
        val result = compileWithProcessor(
            kotlinSource(
                "InheritingDriver.kt",
                """
                package inheriting

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                abstract class BaseDriver : Driver {
                    var lastAction: String = ""

                    @QuintAction("init")
                    open fun init() {
                        lastAction = "base-init"
                    }

                    @QuintAction("untouched")
                    fun untouched() {
                        lastAction = "base-untouched"
                    }
                }

                @QuintRun(spec = "unused.qnt")
                class InheritingDriver : BaseDriver() {
                    override fun step(step: Step) = generatedStep(step)

                    override fun init() {
                        lastAction = "derived-init"
                    }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val driverClass = result.classLoader.loadClass("inheriting.InheritingDriver")
        val driver = driverClass.getDeclaredConstructor().newInstance() as Driver
        val lastActionField = driverClass.superclass.getDeclaredField("lastAction")
            .apply { isAccessible = true }

        driver.step(testStep("untouched"))
        assertEquals("base-untouched", lastActionField.get(driver))

        driver.step(testStep("init"))
        assertEquals("derived-init", lastActionField.get(driver))
    }
}
