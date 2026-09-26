@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
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
}
