@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.quintkonnect.ksp.ir.IR_DIR_OPTION_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Exercises QuintKonnectProcessor's "quintkonnect.irDir" option end to end for a driver whose
// @QuintAction surface already matches its spec, so these tests check that loading the IR never
// affects compilation on its own, whether it's found or not; SpecActionValidatorTest covers a
// found module's own name/param mismatches (qk-75ad), which still leave a miss as a warning.
class QuintIrOptionTest {

    private val driverSource = kotlinSource(
        "IrOptionDriver.kt",
        """
        package iroption

        import io.github.mcbianconi.quintkonnect.Driver
        import io.github.mcbianconi.quintkonnect.annotations.QuintAction
        import io.github.mcbianconi.quintkonnect.annotations.QuintRun

        @QuintRun(spec = "ir/fixture.qnt")
        class IrOptionDriver : Driver {
            @QuintAction("init")
            fun init() {}
        }
        """.trimIndent(),
    )

    @Test
    fun `compiles unchanged when the irDir option is absent`() {
        val result = compileWithProcessor(driverSource)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertFalse(result.messages.contains("quint-konnect"))
    }

    @Test
    fun `loads the IR when the option points at a matching file`(@TempDir tempDir: File) {
        val irFile = File(tempDir, "ir/fixture.qnt.json")
        irFile.parentFile.mkdirs()
        irFile.writeText(File("src/test/resources/ir/fixture.ir.json").readText())

        val result = compileWithProcessor(driverSource, kspOptions = mapOf(IR_DIR_OPTION_NAME to tempDir.absolutePath))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertFalse(result.messages.contains("no quint IR found"))
    }

    @Test
    fun `warns but still compiles when the option is set and no IR file matches`(@TempDir tempDir: File) {
        val result = compileWithProcessor(driverSource, kspOptions = mapOf(IR_DIR_OPTION_NAME to tempDir.absolutePath))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("no quint IR found"))
    }
}
