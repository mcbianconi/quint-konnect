package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.MAX_SAMPLES_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.MAX_STEPS_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.PROJECT_DIR_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.QUINT_EXECUTABLE_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import io.github.mcbianconi.quintkonnect.trace.SEED_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.TestConfig
import io.github.mcbianconi.quintkonnect.trace.genSeed
import io.github.mcbianconi.quintkonnect.trace.maxSamplesOverride
import io.github.mcbianconi.quintkonnect.trace.maxStepsOverride
import io.github.mcbianconi.quintkonnect.trace.quintExecutable
import io.github.mcbianconi.quintkonnect.trace.resolveSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.file.Path

private fun withSystemProperty(name: String, value: String, block: () -> Unit) {
    val previous = System.getProperty(name)
    System.setProperty(name, value)
    try {
        block()
    } finally {
        if (previous == null) System.clearProperty(name) else System.setProperty(name, previous)
    }
}

class GeneratorConfigTest {

    @Test
    fun `resolveSpec returns spec unchanged when projectDir is null`() {
        assertEquals("src/test/foo.qnt", resolveSpec("src/test/foo.qnt", projectDir = null))
    }

    @Test
    fun `resolveSpec resolves a relative spec against projectDir`() {
        assertEquals(
            Path.of("/home/user/project/src/test/foo.qnt").toString(),
            resolveSpec("src/test/foo.qnt", projectDir = "/home/user/project"),
        )
    }

    @Test
    fun `resolveSpec leaves an absolute spec unchanged`() {
        val absolute = Path.of("/tmp/foo.qnt").toString()
        assertEquals(absolute, resolveSpec(absolute, projectDir = "/home/user/project"))
    }

    @Test
    fun `RunConfig toCommand resolves spec against the quintkonnect projectDir system property`() {
        val previous = System.getProperty(PROJECT_DIR_PROPERTY)
        System.setProperty(PROJECT_DIR_PROPERTY, "/home/user/project")
        try {
            val config = RunConfig(spec = "src/test/foo.qnt", seed = "42")
            assertEquals(
                Path.of("/home/user/project/src/test/foo.qnt").toString(),
                config.toCommand(Path.of("tmpdir"))[2],
            )
        } finally {
            if (previous == null) System.clearProperty(PROJECT_DIR_PROPERTY) else System.setProperty(PROJECT_DIR_PROPERTY, previous)
        }
    }

    @Test
    fun `quintExecutable defaults to plain quint when unset`() {
        assertEquals("quint", quintExecutable(quintExecutable = null))
    }

    @Test
    fun `quintExecutable returns the override when present`() {
        assertEquals("/opt/quint-konnect/quint", quintExecutable(quintExecutable = "/opt/quint-konnect/quint"))
    }

    @Test
    fun `RunConfig toCommand uses the quintkonnect quintExecutable system property when set`() {
        val previous = System.getProperty(QUINT_EXECUTABLE_PROPERTY)
        System.setProperty(QUINT_EXECUTABLE_PROPERTY, "/opt/quint-konnect/quint")
        try {
            val config = RunConfig(spec = "foo.qnt", seed = "42")
            assertEquals("/opt/quint-konnect/quint", config.toCommand(Path.of("tmpdir"))[0])
        } finally {
            if (previous == null) System.clearProperty(QUINT_EXECUTABLE_PROPERTY) else System.setProperty(QUINT_EXECUTABLE_PROPERTY, previous)
        }
    }

    @Test
    fun `maxSamplesOverride and maxStepsOverride are absent when unset or non-numeric`() {
        assertNull(maxSamplesOverride(value = null))
        assertNull(maxSamplesOverride(value = "not-a-number"))
        assertNull(maxStepsOverride(value = null))
    }

    @Test
    fun `RunConfig nTraces prefers the maxSamples override over the annotation value`() {
        withSystemProperty(MAX_SAMPLES_PROPERTY, "7") {
            val config = RunConfig(spec = "foo.qnt", seed = "42", maxSamples = 50)
            assertEquals(7, config.nTraces)
            assertEquals("7", config.toCommand(Path.of("tmpdir"))[6])
        }
    }

    @Test
    fun `RunConfig nTraces falls back to the annotation value and then DEFAULT_TRACES`() {
        assertEquals(50, RunConfig(spec = "foo.qnt", seed = "42", maxSamples = 50).nTraces)
        assertEquals(100, RunConfig(spec = "foo.qnt", seed = "42").nTraces)
    }

    @Test
    fun `TestConfig nTraces prefers the maxSamples override over the annotation value`() {
        withSystemProperty(MAX_SAMPLES_PROPERTY, "3") {
            val config = TestConfig(spec = "foo.qnt", test = "t", seed = "42", maxSamples = 50)
            assertEquals(3, config.nTraces)
        }
    }

    @Test
    fun `RunConfig toCommand adds --max-steps from the override even without an annotation value`() {
        withSystemProperty(MAX_STEPS_PROPERTY, "12") {
            val config = RunConfig(spec = "foo.qnt", seed = "42")
            val command = config.toCommand(Path.of("tmpdir"))
            assertEquals("--max-steps", command[command.size - 2])
            assertEquals("12", command.last())
        }
    }

    @Test
    fun `RunConfig toCommand's max-steps override wins over the annotation value`() {
        withSystemProperty(MAX_STEPS_PROPERTY, "12") {
            val config = RunConfig(spec = "foo.qnt", seed = "42", maxSteps = 99)
            val command = config.toCommand(Path.of("tmpdir"))
            assertEquals("12", command.last())
        }
    }

    @Test
    fun `genSeed returns the quintkonnect seed override when set`() {
        withSystemProperty(SEED_PROPERTY, "0xoverride") {
            assertEquals("0xoverride", genSeed())
        }
    }
}
