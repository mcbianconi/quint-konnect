@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import io.github.mcbianconi.quintkonnect.QuintSuite
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Exercises QuintKonnectProcessor's "quintkonnect.adapter" option end to end (QuintSuiteGenerator):
// "junit" (the default) generates both the runner-neutral QuintSuite and its JUnit adapter,
// "none" generates only the suite, and any other value is a compile error.
class QuintSuiteAdapterOptionTest {

    private fun driverSource(pkg: String) = kotlinSource(
        "AdapterOptionDriver.kt",
        """
        package $pkg

        import io.github.mcbianconi.quintkonnect.Driver
        import io.github.mcbianconi.quintkonnect.Step
        import io.github.mcbianconi.quintkonnect.annotations.QuintRun

        @QuintRun(spec = "unused.qnt")
        class AdapterOptionDriver : Driver {
            override fun step(step: Step) {}
        }
        """.trimIndent(),
    )

    // "inheritClassPath = true" (CompileTestSupport.compileWithProcessor) always puts JUnit on the
    // classpath, so a compile check alone can't prove the adapter was skipped; check the generated
    // source text itself instead.
    private fun generatedKotlinSourceFiles(result: JvmCompilationResult): List<java.io.File> =
        result.outputDirectory.parentFile.resolve("ksp/sources/kotlin")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    @Test
    fun `defaults to the junit adapter when the option is absent`() {
        val result = compileWithProcessor(driverSource("adapterdefault"))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        result.classLoader.loadClass("adapterdefault.AdapterOptionDriverQuintSuite")
        result.classLoader.loadClass("adapterdefault.AdapterOptionDriverQuintRunTest")
    }

    @Test
    fun `generates only the suite when the option is none`() {
        val result = compileWithProcessor(
            driverSource("adapternone"),
            kspOptions = mapOf(ADAPTER_OPTION_NAME to "none"),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val suiteClass = result.classLoader.loadClass("adapternone.AdapterOptionDriverQuintSuite")
        val suite = suiteClass.getField("INSTANCE").get(null) as QuintSuite
        assertEquals("AdapterOptionDriver", suite.name)

        assertThrows(ClassNotFoundException::class.java) {
            result.classLoader.loadClass("adapternone.AdapterOptionDriverQuintRunTest")
        }

        val sources = generatedKotlinSourceFiles(result)
        assertTrue(sources.isNotEmpty())
        sources.forEach { file ->
            assertFalse(file.readText().contains("org.junit"), "${file.name} unexpectedly references org.junit")
        }
    }

    @Test
    fun `generates the junit adapter when the option is explicitly junit`() {
        val result = compileWithProcessor(
            driverSource("adapterjunit"),
            kspOptions = mapOf(ADAPTER_OPTION_NAME to "junit"),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        result.classLoader.loadClass("adapterjunit.AdapterOptionDriverQuintSuite")
        result.classLoader.loadClass("adapterjunit.AdapterOptionDriverQuintRunTest")
    }

    @Test
    fun `an invalid adapter value is a compile error naming the valid values`() {
        val result = compileWithProcessor(
            driverSource("adapterinvalid"),
            kspOptions = mapOf(ADAPTER_OPTION_NAME to "kotest"),
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains(ADAPTER_OPTION_NAME), result.messages)
        assertTrue(result.messages.contains("junit"), result.messages)
        assertTrue(result.messages.contains("none"), result.messages)
    }
}
