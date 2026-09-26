@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.ParameterizedType

class QuintRunTestGeneratorTest {

    private companion object {
        // Kotlin source text for a spec path containing '$' and '"', mirroring the escaping
        // fixture in example/.../escaping/EscapingCounterDriver.kt (qk-gu38), which uses
        // `spec = "src/test/resources/escaping/counter\$1.qnt"`.
        val escapedSpecLiteral = "\"escaping/counter\\\$1.qnt\""

        val result = compileWithProcessor(
            kotlinSource(
                "RunDriver.kt",
                """
                package run1

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(
                    spec = __SPEC__,
                    main = "Main",
                    init = "init",
                    step = "step",
                    maxSamples = 3,
                    maxSteps = 5,
                    seed = "cafe",
                )
                class RunDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent().replace("__SPEC__", escapedSpecLiteral),
            ),
        )
    }

    @Test
    fun `compiles a QuintRun test class with escaped annotation args`() {
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `generates a JUnit test class with a public no-arg constructor and a TestFactory-annotated traces method`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val testClass = result.classLoader.loadClass("run1.RunDriverQuintRunTest")
        testClass.getDeclaredConstructor()

        val tracesMethod = testClass.getDeclaredMethod("traces")
        assertTrue(tracesMethod.isAnnotationPresent(org.junit.jupiter.api.TestFactory::class.java))
        assertTrue(java.util.Collection::class.java.isAssignableFrom(tracesMethod.returnType))
        val elementType = (tracesMethod.genericReturnType as ParameterizedType).actualTypeArguments[0]
        assertEquals(DynamicTest::class.java, elementType)

        assertThrows<NoSuchMethodException> { testClass.getDeclaredMethod("run") }
    }

    @Test
    fun `generates a test class using the default seed when none is given`() {
        val defaultsResult = compileWithProcessor(
            kotlinSource(
                "DefaultsDriver.kt",
                """
                package run2

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintRun

                @QuintRun(spec = "unused.qnt")
                class DefaultsDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, defaultsResult.exitCode, defaultsResult.messages)

        val testClass = defaultsResult.classLoader.loadClass("run2.DefaultsDriverQuintRunTest")
        testClass.getDeclaredConstructor()
        val tracesMethod = testClass.getDeclaredMethod("traces")
        assertTrue(tracesMethod.isAnnotationPresent(org.junit.jupiter.api.TestFactory::class.java))
    }
}
