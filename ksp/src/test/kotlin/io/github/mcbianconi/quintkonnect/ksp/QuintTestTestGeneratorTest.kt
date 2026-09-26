@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
    fun `generates a JUnit test class with a public no-arg constructor and a Test-annotated run method`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val testClass = result.classLoader.loadClass("test1.TestDriverQuintTestTest")
        testClass.getDeclaredConstructor()

        val runMethod = testClass.getDeclaredMethod("run")
        assertTrue(runMethod.isAnnotationPresent(org.junit.jupiter.api.Test::class.java))
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
        val runMethod = testClass.getDeclaredMethod("run")
        assertTrue(runMethod.isAnnotationPresent(org.junit.jupiter.api.Test::class.java))
    }
}
