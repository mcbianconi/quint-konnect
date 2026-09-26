package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.TraceGenerator
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TraceGeneratorTest {

    @Test
    fun `drain large stdout and stderr without deadlock`() {
        val script = "head -c 2097152 /dev/zero; head -c 2097152 /dev/zero >&2"

        val config = SimpleTestConfig(script)
        val traces = TraceGenerator.generate(config)
        assertTrue(traces.isEmpty())
    }

    @Test
    fun `timeout on long-running process`() {
        val script = "sleep 30"
        val config = SimpleTestConfig(script, timeout = 500.milliseconds)

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("did not finish within") == true)
    }

    @Test
    fun `capture stderr on non-zero exit`() {
        val script = "echo 'Error message' >&2; exit 1"

        val config = SimpleTestConfig(script)

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("non-zero exit code") == true)
        assertTrue(exception.message?.contains("Error message") == true)
    }

    private class SimpleTestConfig(
        private val script: String,
        override val timeout: Duration = Duration.INFINITE,
    ) : GeneratorConfig {
        override val seed: String = "test-seed"
        override val nTraces: Int = 0

        override fun toCommand(tmpDir: Path): List<String> =
            listOf("sh", "-c", script)
    }
}
