@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.ParameterizedType

class QuintTestTestGeneratorTest {

    private companion object {
        // Mirrors the escaping used in QuintRunTestGeneratorTest / the example fixture (qk-gu38).
        val escapedTestLiteral = "\"my\\\$test\\\"name\""

        val result = compileWithProcessor(
            kotlinSource(
                "TestDriver.kt",
                """
                package test1

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintTest

                @QuintTest(
                    spec = "unused.qnt",
                    test = __TEST__,
                    main = "Main",
                    maxSamples = 3,
                    seed = "cafe",
                )
                class TestDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent().replace("__TEST__", escapedTestLiteral),
            ),
        )
    }

    @Test
    fun `compiles a QuintTest test class with escaped annotation args`() {
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `generates a JUnit test class with a public no-arg constructor and a TestFactory-annotated traces method`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val testClass = result.classLoader.loadClass("test1.TestDriverQuintTestTest")
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
                "DefaultsTestDriver.kt",
                """
                package test2

                import io.github.mcbianconi.quintkonnect.Driver
                import io.github.mcbianconi.quintkonnect.Step
                import io.github.mcbianconi.quintkonnect.annotations.QuintTest

                @QuintTest(spec = "unused.qnt", test = "counterTest")
                class DefaultsTestDriver : Driver {
                    override fun step(step: Step) {}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(KotlinCompilation.ExitCode.OK, defaultsResult.exitCode, defaultsResult.messages)

        val testClass = defaultsResult.classLoader.loadClass("test2.DefaultsTestDriverQuintTestTest")
        testClass.getDeclaredConstructor()
        val tracesMethod = testClass.getDeclaredMethod("traces")
        assertTrue(tracesMethod.isAnnotationPresent(org.junit.jupiter.api.TestFactory::class.java))
    }
}
