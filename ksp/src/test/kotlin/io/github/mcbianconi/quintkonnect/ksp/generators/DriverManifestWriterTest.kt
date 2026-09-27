@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp.generators

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.quintkonnect.ksp.compileWithProcessor
import io.github.mcbianconi.quintkonnect.ksp.kotlinSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

// generateQuintTraces (gradle-plugin, qk-adm2) reads these manifests to run `quint` once per
// driver at Gradle-task time; this proves KSP actually writes them with the right fields (the
// Gradle-side task itself is tested in :gradle-plugin against hand-written fixture manifests, per
// its own functional test comment on why it can't compile a real driver against unpublished
// core/ksp artifacts).
class DriverManifestWriterTest {

    private fun manifestsOf(result: com.tschuchort.compiletesting.JvmCompilationResult): List<kotlinx.serialization.json.JsonObject> =
        result.outputDirectory.parentFile.resolve("ksp/sources/resources")
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".json") }
            .map { Json.parseToJsonElement(it.readText()).jsonObject }
            .toList()

    @Test
    fun `writes a run manifest with every field`() {
        val result = compileWithProcessor(
            kotlinSource(
                "ManifestRunDriver.kt",
                """
                package manifestrun

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(
                    spec = "spec.qnt",
                    main = "Main",
                    init = "init",
                    step = "step",
                    maxSamples = 3,
                    maxSteps = 5,
                    seed = "cafe",
                    invariants = ["safe"],
                )
                class ManifestRunDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val manifest = manifestsOf(result).single { it["driver"]?.jsonPrimitive?.contentOrNull == "manifestrun.ManifestRunDriver" }

        assertEquals("run", manifest["kind"]?.jsonPrimitive?.contentOrNull)
        assertEquals("spec.qnt", manifest["spec"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Main", manifest["main"]?.jsonPrimitive?.contentOrNull)
        assertEquals("init", manifest["init"]?.jsonPrimitive?.contentOrNull)
        assertEquals("step", manifest["step"]?.jsonPrimitive?.contentOrNull)
        assertNull(manifest["test"])
        assertEquals(3, manifest["maxSamples"]?.jsonPrimitive?.content?.toInt())
        assertEquals(5, manifest["maxSteps"]?.jsonPrimitive?.content?.toInt())
        assertEquals("cafe", manifest["seed"]?.jsonPrimitive?.contentOrNull)
        assertEquals(listOf("safe"), manifest["invariants"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun `writes a test manifest with every field`() {
        val result = compileWithProcessor(
            kotlinSource(
                "ManifestTestDriver.kt",
                """
                package manifesttest

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintTest

                @QuintTest(spec = "spec.qnt", test = "aTest", main = "Main", maxSamples = 7, seed = "beef")
                class ManifestTestDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val manifest = manifestsOf(result).single { it["driver"]?.jsonPrimitive?.contentOrNull == "manifesttest.ManifestTestDriver" }

        assertEquals("test", manifest["kind"]?.jsonPrimitive?.contentOrNull)
        assertEquals("spec.qnt", manifest["spec"]?.jsonPrimitive?.contentOrNull)
        assertEquals("aTest", manifest["test"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Main", manifest["main"]?.jsonPrimitive?.contentOrNull)
        assertNull(manifest["init"])
        assertNull(manifest["step"])
        assertNull(manifest["maxSteps"])
        assertEquals(7, manifest["maxSamples"]?.jsonPrimitive?.content?.toInt())
        assertEquals("beef", manifest["seed"]?.jsonPrimitive?.contentOrNull)
        assertTrue(manifest["invariants"]!!.jsonArray.isEmpty())
    }

    @Test
    fun `omits blank optional fields instead of writing empty strings`() {
        val result = compileWithProcessor(
            kotlinSource(
                "ManifestDefaultsDriver.kt",
                """
                package manifestdefaults

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "spec.qnt")
                class ManifestDefaultsDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val manifest = manifestsOf(result).single { it["driver"]?.jsonPrimitive?.contentOrNull == "manifestdefaults.ManifestDefaultsDriver" }

        assertNull(manifest["main"])
        assertNull(manifest["init"])
        assertNull(manifest["step"])
        assertNull(manifest["maxSamples"])
        assertNull(manifest["maxSteps"])
        assertNull(manifest["seed"])
    }
}
