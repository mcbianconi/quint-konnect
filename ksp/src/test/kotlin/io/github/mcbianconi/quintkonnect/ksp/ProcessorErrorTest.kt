@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ProcessorErrorTest {

    // QuintKonnectProcessor (ksp/src/main/.../QuintKonnectProcessor.kt) never calls
    // KSPLogger.error: it has no validation for duplicate @QuintAction names or unsupported
    // parameter shapes. This test documents the resulting behaviour for duplicate names: the
    // generator emits two `when` branches with the same string label, which the Kotlin compiler
    // does not reject, so the first branch silently wins. See qk-nqry.
    @Disabled("qk-nqry: duplicate @QuintAction names silently shadow instead of being rejected")
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
    }

    @Test
    fun `duplicate action names currently compile and the first branch wins`() {
        val result = compileWithProcessor(
            kotlinSource(
                "DuplicateActionDriver2.kt",
                """
                package dup2

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class DuplicateActionDriver2 : Driver {
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

        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val clazz = result.classLoader.loadClass("dup2.DuplicateActionDriver2")
        val driver = clazz.getDeclaredConstructor().newInstance() as Driver
        driver.step(testStep("dup"))

        val lastAction = clazz.getDeclaredField("lastAction").apply { isAccessible = true }.get(driver)
        assertEquals("first", lastAction)
    }
}
