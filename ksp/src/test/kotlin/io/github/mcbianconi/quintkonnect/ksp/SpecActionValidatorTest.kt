@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.github.mcbianconi.quintkonnect.ksp.ir.IR_DIR_OPTION_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// qk-75ad: QuintKonnectProcessor.loadIr's validateAgainstSpec call, exercised end to end through
// real `quint typecheck --out` fixtures (ksp/src/test/resources/ir/*.ir.json; each has the command
// that produced it next to it).
class SpecActionValidatorTest {

    // Copies a checked-in "<name>.ir.json" fixture to "<tempDir>/ir/<name>.qnt.json", matching
    // loadQuintIrModule's "<irDir>/<specPath>.json" naming for a driver using `spec = "ir/<name>.qnt"`.
    private fun irDirWith(tempDir: File, name: String): String {
        val irFile = File(tempDir, "ir/$name.qnt.json")
        irFile.parentFile.mkdirs()
        irFile.writeText(File("src/test/resources/ir/$name.ir.json").readText())
        return tempDir.absolutePath
    }

    private fun compile(source: SourceFile, tempDir: File, irName: String): JvmCompilationResult =
        compileWithProcessor(source, kspOptions = mapOf(IR_DIR_OPTION_NAME to irDirWith(tempDir, irName)))

    @Test
    fun `a driver whose @QuintAction surface matches tictactoe's spec compiles with no errors or warnings`(@TempDir tempDir: File) {
        // Mirrors example/.../tictactoe/TicTacToeDriver.kt's @QuintAction surface exactly (minus
        // game logic and the State override, unrelated to this check): "pattern" is a real nondet
        // of MoveX (transitively, via Win/Block) that the driver doesn't use as a parameter, and
        // Win/Block/StartInCorner/TakeCenter/SetupWin/Move are helper actions never directly
        // dispatched (only reached through `if`, not `any {...}`), so neither should warn.
        val result = compile(
            kotlinSource(
                "TicTacToeLikeDriver.kt",
                """
                package tictactoelike

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/tictactoe.qnt")
                class TicTacToeLikeDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("MoveX")
                    fun moveX(corner: List<Long>?, coordinate: List<Long>?) {}

                    @QuintAction("MoveO")
                    fun moveO(coordinate: List<Long>) {}

                    @QuintAction("stuttered")
                    fun stuttered() {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "tictactoe",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertFalse(result.messages.contains("quint-konnect"), result.messages)
    }

    @Test
    fun `a @QuintAction name not in the spec is a compile error`(@TempDir tempDir: File) {
        val result = compile(
            kotlinSource(
                "TypoDriver.kt",
                """
                package typo

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/fixture.qnt")
                class TypoDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("pikc")
                    fun pick(n: Long) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "fixture",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("does not match any action"), result.messages)
    }

    @Test
    fun `a parameter name not among the action's nondets is a compile error`(@TempDir tempDir: File) {
        val result = compile(
            kotlinSource(
                "WrongParamNameDriver.kt",
                """
                package wrongparamname

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/fixture.qnt")
                class WrongParamNameDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("pick")
                    fun pick(wrongName: Long) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "fixture",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("is not a nondet of that action"), result.messages)
    }

    @Test
    fun `a clearly incompatible parameter type is a compile error`(@TempDir tempDir: File) {
        val result = compile(
            kotlinSource(
                "WrongTypeDriver.kt",
                """
                package wrongtype

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/fixture.qnt")
                class WrongTypeDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("pick")
                    fun pick(n: String) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "fixture",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("doesn't decode from the spec's nondet type"), result.messages)
    }

    @Test
    fun `a dispatchable spec action with no @QuintAction is a compile warning, not an error`(@TempDir tempDir: File) {
        // dispatch.qnt's `step = any { addOne, let n = ...; addTwo(n), nested }` makes addOne and
        // addTwo (via "nested") both directly dispatchable; this driver only covers addOne.
        val result = compile(
            kotlinSource(
                "PartialDispatchDriver.kt",
                """
                package partialdispatch

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/dispatch.qnt")
                class PartialDispatchDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("addOne")
                    fun addOne() {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "dispatch",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("spec action \"addTwo\""), result.messages)
        assertTrue(result.messages.contains("has no @QuintAction"), result.messages)
    }

    @Test
    fun `a nondet bound in step's own let, not the dispatched action's body, still resolves for that action`(@TempDir tempDir: File) {
        // dispatch.qnt's addTwo(n) call is `any { ...; nondet n = ...; addTwo(n), ... }`: "n" is
        // bound in step's own body, not addTwo's (mbt::nondetPicks is step-scoped, confirmed
        // against a real `quint run --mbt` trace), so this must not error.
        val result = compile(
            kotlinSource(
                "FullDispatchDriver.kt",
                """
                package fulldispatch

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/dispatch.qnt")
                class FullDispatchDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("addOne")
                    fun addOne() {}

                    @QuintAction("addTwo")
                    fun addTwo(n: Long) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "dispatch",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertFalse(result.messages.contains("quint-konnect"), result.messages)
    }

    @Test
    fun `a type mismatch on a nondet bound outside the dispatched action's own body is still a compile error`(@TempDir tempDir: File) {
        val result = compile(
            kotlinSource(
                "WrongEntryNondetTypeDriver.kt",
                """
                package wrongentrynondettype

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/dispatch.qnt")
                class WrongEntryNondetTypeDriver : Driver {
                    @QuintAction("init")
                    fun init() {}

                    @QuintAction("addOne")
                    fun addOne() {}

                    @QuintAction("addTwo")
                    fun addTwo(n: String) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "dispatch",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("doesn't decode from the spec's nondet type"), result.messages)
    }

    @Test
    fun `a driver overriding config() is not validated against the spec`(@TempDir tempDir: File) {
        // Mirrors example/.../sumtypes/VendingMachineDriver.kt: a @QuintRun driver that still
        // reads actionTaken from a hand-modeled sum type via DriverConfig.nondetPath, so its
        // @QuintAction names ("pikc" here, deliberately not in fixture.qnt's actions) are sum-type
        // variant tags, not module.actions keys -- this must not error.
        val result = compile(
            kotlinSource(
                "NondetPathDriver.kt",
                """
                package nondetpath

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.DriverConfig
                import io.github.mcbianconi.quintkonnect.annotations.QuintAction
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "ir/fixture.qnt")
                class NondetPathDriver : Driver {
                    override fun config(): DriverConfig = DriverConfig(nondetPath = listOf("lastChoice"))

                    @QuintAction("pikc")
                    fun pick(n: Long) {}
                }
                """.trimIndent(),
            ),
            tempDir,
            "fixture",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertFalse(result.messages.contains("quint-konnect"), result.messages)
    }
}
