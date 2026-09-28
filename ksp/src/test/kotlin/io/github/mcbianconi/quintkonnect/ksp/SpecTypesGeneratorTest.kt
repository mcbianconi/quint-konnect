@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.github.mcbianconi.quintkonnect.ksp.ir.IR_DIR_OPTION_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// qk-ixox: SpecTypesGenerator, driven through the processor with checked-in `quint typecheck --out`
// fixtures (ksp/src/test/resources/ir/*.ir.json). Decoding the generated types against real ITF
// values needs the kotlinx-serialization compiler plugin, which these compilations don't apply;
// example/.../tictactoe/TicTacToeDriver.kt covers that end to end.
class SpecTypesGeneratorTest {

    private fun irDirWith(tempDir: File, name: String): String {
        val irFile = File(tempDir, "ir/$name.qnt.json")
        irFile.parentFile.mkdirs()
        irFile.writeText(File("src/test/resources/ir/$name.ir.json").readText())
        return tempDir.absolutePath
    }

    private fun compile(tempDir: File, irName: String, vararg sources: SourceFile): JvmCompilationResult =
        compileWithProcessor(*sources, kspOptions = mapOf(IR_DIR_OPTION_NAME to irDirWith(tempDir, irName)))

    private fun JvmCompilationResult.generated(fileName: String): String =
        sourcesGeneratedBySymbolProcessor.single { it.name == fileName }.readText()

    private fun driver(pkg: String, name: String, spec: String, body: String = "", ignore: List<String> = emptyList()) = kotlinSource(
        "$name.kt",
        """
        package $pkg

        import io.github.mcbianconi.quintkonnect.Driver
        import io.github.mcbianconi.quintkonnect.annotations.QuintAction
        import io.github.mcbianconi.quintkonnect.annotations.QuintRun

        @QuintRun(spec = "$spec"${if (ignore.isEmpty()) "" else ", ignore = [${ignore.joinToString(", ") { "\"$it\"" }}]"})
        class $name : Driver {
            @QuintAction("init")
            fun init() {}
            $body
        }
        """.trimIndent(),
    )

    @Test
    fun `generates records, sums, Option, imported typedefs and the state class`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "spectypes",
            driver(
                "shapes",
                "ShapesDriver",
                "ir/spectypes.qnt",
                """
                @QuintAction("paint")
                fun paint(s: SpectypesSpec.Shape, at: SpectypesSpec.At) {}
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val source = result.generated("SpectypesSpec.kt")
        val expected = listOf(
            "public data class Coin(\n    public val denom: String,\n    public val amount: Long,\n  )",
            "public sealed class Shape",
            "@SerialName(\"Circle\")\n    public data class Circle(\n      public val `value`: ShapeCircle,\n    ) : Shape()",
            "@SerialName(\"Square\")\n    public data class Square(\n      public val `value`: Long,\n    ) : Shape()",
            "@Serializable\n    @SerialName(\"Dot\")\n    public data object Dot : Shape()",
            "public data class ShapeCircle(\n    public val r: Long,\n  )",
            "public val wallet: Coin,",
            "public val maybe: Long?,",
            "public val shapes: Set<Shape>,",
            "public val grid: Map<List<Long>, Shape>,",
            "public val owner: Owner,",
            "public val flags: List<Boolean>,",
            "public data class Owner(\n    public val name: String,\n    public val tags: Set<String>,\n  )",
            "public data class At(\n    public val x: Long,\n    public val y: Long,\n  )",
            // quint expands an applied generic (`Opt[Coin]`) into its sum before KSP sees it, so it
            // is recovered as an application of the generic typedef and named after it.
            "public val mine: OptCoin,",
            "public data class Purse(\n    public val held: OptCoin,\n  )",
            "@SerialName(\"Present\")\n    public data class Present(\n      public val `value`: Coin,\n    ) : OptCoin()",
        )
        expected.forEach { assertTrue(it in source, "missing:\n$it\n\nin:\n$source") }
        assertFalse("class Opt(" in source || "class Opt " in source, source)
        assertFalse("class Option" in source, source)
        assertFalse("class Mixed" in source, source)
        // The nondet `s` reads as Shape's expanded structure in quint's inferred types; it must
        // reuse Shape, not add a structural duplicate named after the nondet.
        assertFalse("class S " in source, source)
        assertTrue("SpectypesSpec leaves out spec types" in result.messages, result.messages)
        assertTrue("type Mixed: tuple (int, str) mixes element types" in result.messages, result.messages)
    }

    @Test
    fun `nests spec types so a same-named implementation class in the package doesn't clash`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "tictactoe",
            kotlinSource("Player.kt", "package ttt\n\nenum class Player { X, O }"),
            driver(
                "ttt",
                "TttDriver",
                "ir/tictactoe.qnt",
                """
                @QuintAction("MoveX")
                fun moveX(corner: List<Long>?, coordinate: List<Long>?) {}
                @QuintAction("MoveO")
                fun moveO(coordinate: List<Long>) {}
                @QuintAction("stuttered")
                fun stuttered() {}
                val next: Player = Player.X
                val spec: TictactoeSpec.Player = TictactoeSpec.Player.X
                val state: TictactoeSpec.State? = null
                """,
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val source = result.generated("TictactoeSpec.kt")
        assertTrue("public val board: Map<Long, Map<Long, Square>>," in source, source)
        assertTrue("public val nextTurn: Player," in source, source)
        assertFalse("quint-konnect" in result.messages, result.messages)
    }

    @Test
    fun `drivers sharing a spec in one package get one generated file`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "spectypes",
            driver("shared", "FirstDriver", "ir/spectypes.qnt", "@QuintAction(\"paint\") fun paint() {}"),
            driver("shared", "SecondDriver", "ir/spectypes.qnt", "@QuintAction(\"paint\") fun paint() {}"),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals(1, result.sourcesGeneratedBySymbolProcessor.count { it.name == "SpectypesSpec.kt" })
    }

    // qk-sr50: KSP's incremental mode reprocesses every originating file of a dirty output, so a
    // shared <Module>Spec.kt must list each driver using it; with only the first, deleting that
    // driver would drop the file the second still needs.
    @Test
    fun `a shared spec types file lists every driver using it as an originating file`(@TempDir tempDir: File) {
        val originsByFile = mutableMapOf<String, List<String>>()
        val provider = SymbolProcessorProvider { env ->
            val recording = object : CodeGenerator by env.codeGenerator {
                override fun createNewFile(dependencies: Dependencies, packageName: String, fileName: String, extensionName: String) =
                    env.codeGenerator.createNewFile(dependencies, packageName, fileName, extensionName).also {
                        originsByFile[fileName] = dependencies.originatingFiles.map { it.fileName }.sorted()
                    }
            }
            QuintKonnectProcessorProvider().create(
                SymbolProcessorEnvironment(
                    env.options,
                    env.kotlinVersion,
                    recording,
                    env.logger,
                    env.apiVersion,
                    env.compilerVersion,
                    env.platforms,
                    env.kspVersion,
                ),
            )
        }
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                driver("shared", "FirstDriver", "ir/spectypes.qnt", "@QuintAction(\"paint\") fun paint() {}"),
                driver("shared", "SecondDriver", "ir/spectypes.qnt", "@QuintAction(\"paint\") fun paint() {}"),
            )
            inheritClassPath = true
            jvmTarget = "21"
            useKsp2()
            symbolProcessorProviders = mutableListOf(provider)
            messageOutputStream = System.out
            kspProcessorOptions = mutableMapOf(IR_DIR_OPTION_NAME to irDirWith(tempDir, "spectypes"))
        }

        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals(listOf("FirstDriver.kt", "SecondDriver.kt"), originsByFile["SpectypesSpec"])
    }

    // qk-ymex: `ignore` (an @QuintRun/@QuintTest annotation parameter) drives a per-driver
    // `<Driver>State` next to the shared `State`, so a driver that doesn't model every variable
    // can still project onto a KSP-generated class instead of hand-writing one.
    @Test
    fun `ignore generates a driver-specific state dropping the ignored variable`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "fixture",
            driver("fx", "FxDriver", "ir/fixture.qnt", ignore = listOf("lastChoice")),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val source = result.generated("FixtureSpec.kt")
        assertTrue(
            "public data class FxDriverState(\n    public val `value`: Long,\n    public val config: Config,\n  )" in source,
            source,
        )
        // The canonical State still has every variable, lastChoice included.
        assertTrue("public val lastChoice: Choice," in source, source)
    }

    @Test
    fun `ignoring an unsupported variable still generates the driver-specific state`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "unsupportedvar",
            driver("uv", "UvDriver", "ir/unsupportedvar.qnt", ignore = listOf("pair")),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val source = result.generated("UnsupportedvarSpec.kt")
        assertTrue("public data class UvDriverState(\n    public val `value`: Long,\n  )" in source, source)
        // The canonical State is skipped: `pair` has no decodable shape.
        assertTrue("UnsupportedvarSpec leaves out spec types" in result.messages, result.messages)
        assertTrue("tuple (int, str) mixes element types" in result.messages, result.messages)
    }

    @Test
    fun `an unknown ignore name is a compile error`(@TempDir tempDir: File) {
        val result = compile(
            tempDir,
            "fixture",
            driver("fx2", "Fx2Driver", "ir/fixture.qnt", ignore = listOf("nope")),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            "ignore names unknown state variable(s) [nope]" in result.messages,
            result.messages,
        )
    }
}
