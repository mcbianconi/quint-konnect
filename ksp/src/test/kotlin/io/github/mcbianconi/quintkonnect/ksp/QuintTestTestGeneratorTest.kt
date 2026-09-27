@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.nio.file.Path

// Kept as literals: quintkonnect.seed/quintkonnect.quintExecutable are `internal` to :core
// (SEED_PROPERTY, QUINT_EXECUTABLE_PROPERTY in trace/Seed.kt, trace/GeneratorConfig.kt).
private const val SEED_OVERRIDE_PROPERTY = "quintkonnect.seed"
private const val QUINT_EXECUTABLE_PROPERTY = "quintkonnect.quintExecutable"

private fun withSystemProperty(name: String, value: String, block: () -> Unit) {
    val previous = System.getProperty(name)
    System.setProperty(name, value)
    try {
        block()
    } finally {
        if (previous == null) System.clearProperty(name) else System.setProperty(name, previous)
    }
}

// Stands in for the real `quint` CLI so `traces()` can run without it (not required for
// :ksp:test, see AGENTS.md): captures the argv quint-konnect invokes it with into [argsFile] and
// exits successfully with no `--out-itf` files written, so `ReplayRunner.traceReplays` sees zero
// traces and returns a single non-throwing "no traces generated" DynamicTest.
private fun writeFakeQuintExecutable(argsFile: Path): File {
    val script = File.createTempFile("fake-quint", "")
    script.writeText("#!/bin/sh\nprintf '%s\\n' \"${'$'}@\" > '$argsFile'\n")
    script.setExecutable(true)
    script.deleteOnExit()
    return script
}

private fun readSeedArgument(argsFile: Path): String {
    val args = Files.readAllLines(argsFile)
    return args[args.indexOf("--seed") + 1]
}

class QuintTestTestGeneratorTest {

    private companion object {
        // Mirrors the escaping used in QuintRunTestGeneratorTest / the integration-tests fixture (qk-gu38).
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

        val execution = tracesMethod.getAnnotation(org.junit.jupiter.api.parallel.Execution::class.java)
        assertEquals(org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT, execution.value)

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

    @Test
    fun `bakes the annotation seed into the generated TestConfig when no override is set`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val testClass = result.classLoader.loadClass("test1.TestDriverQuintTestTest")
        val instance = testClass.getDeclaredConstructor().newInstance()
        val tracesMethod = testClass.getDeclaredMethod("traces")

        val argsFile = Files.createTempFile("quint-args", ".txt")
        val fakeQuint = writeFakeQuintExecutable(argsFile)
        withSystemProperty(QUINT_EXECUTABLE_PROPERTY, fakeQuint.absolutePath) {
            tracesMethod.invoke(instance)
        }

        assertEquals("cafe", readSeedArgument(argsFile))
    }

    @Test
    fun `-Pquint_seed override wins over the baked annotation seed at runtime`() {
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }

        val testClass = result.classLoader.loadClass("test1.TestDriverQuintTestTest")
        val instance = testClass.getDeclaredConstructor().newInstance()
        val tracesMethod = testClass.getDeclaredMethod("traces")

        val argsFile = Files.createTempFile("quint-args", ".txt")
        val fakeQuint = writeFakeQuintExecutable(argsFile)
        withSystemProperty(QUINT_EXECUTABLE_PROPERTY, fakeQuint.absolutePath) {
            withSystemProperty(SEED_OVERRIDE_PROPERTY, "0xoverride") {
                tracesMethod.invoke(instance)
            }
        }

        assertEquals("0xoverride", readSeedArgument(argsFile))
    }
}
