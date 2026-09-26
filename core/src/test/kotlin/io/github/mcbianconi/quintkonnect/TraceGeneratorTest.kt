package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.trace.GeneratorConfig
import io.github.mcbianconi.quintkonnect.trace.TraceGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
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

    @Test
    fun `reads traces written to tmpDir back sorted by file name`() {
        val config = object : GeneratorConfig {
            override val seed = "test-seed"
            override val nTraces = 0
            override fun toCommand(tmpDir: Path): List<String> {
                val bFile = tmpDir.resolve("b.itf.json")
                val aFile = tmpDir.resolve("a.itf.json")
                val script = "echo '{\"states\": [{\"x\": 2}]}' > '$bFile'\n" +
                    "echo '{\"states\": [{\"x\": 1}]}' > '$aFile'"
                return listOf("sh", "-c", script)
            }
        }

        val traces = TraceGenerator.generate(config)

        assertEquals(2, traces.size)
        val xValues = traces.map { (it.states.first().value["x"] as ItfValue.Num).value }
        assertEquals(listOf(1L, 2L), xValues)
    }

    @Test
    fun `deletes the tmp directory after a successful run`() {
        var capturedTmpDir: Path? = null
        val config = object : GeneratorConfig {
            override val seed = "test-seed"
            override val nTraces = 0
            override fun toCommand(tmpDir: Path): List<String> {
                capturedTmpDir = tmpDir
                return listOf("sh", "-c", "echo '{\"states\": []}' > '${tmpDir.resolve("a.itf.json")}'")
            }
        }

        TraceGenerator.generate(config)

        assertTrue(Files.notExists(capturedTmpDir!!))
    }

    @Test
    fun `deletes the tmp directory even when the process fails`() {
        var capturedTmpDir: Path? = null
        val config = object : GeneratorConfig {
            override val seed = "test-seed"
            override val nTraces = 0
            override fun toCommand(tmpDir: Path): List<String> {
                capturedTmpDir = tmpDir
                return listOf("sh", "-c", "echo '{\"states\": []}' > '${tmpDir.resolve("a.itf.json")}'; exit 1")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(Files.notExists(capturedTmpDir!!))
    }

    @Test
    fun `names a single violated invariant and the seed without needing the stdout breakdown`() {
        val script = "echo 'error: Invariant violated' >&2; exit 1"
        val config = SimpleTestConfig(script, invariants = listOf("safe"))

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("Quint invariant violated: safe") == true)
        assertTrue(exception.message?.contains("seed test-seed") == true)
    }

    @Test
    fun `names only the invariants quint reports as violated when several were checked`() {
        val script = "echo '  ❌ bar' ; echo 'error: Invariant violated' >&2; exit 1"
        val config = SimpleTestConfig(script, invariants = listOf("foo", "bar"))

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertEquals("Quint invariant violated: bar (seed test-seed)", exception.message)
    }

    @Test
    fun `falls back to naming every configured invariant when quint's breakdown can't be parsed`() {
        val script = "echo 'error: Invariant violated' >&2; exit 1"
        val config = SimpleTestConfig(script, invariants = listOf("foo", "bar"))

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("Quint invariant violated: foo, bar") == true)
    }

    @Test
    fun `attaches the violating trace quint wrote before exiting`() {
        val config = object : GeneratorConfig {
            override val seed = "test-seed"
            override val nTraces = 0
            override val invariants = listOf("safe")
            override fun toCommand(tmpDir: Path): List<String> {
                val itfFile = tmpDir.resolve("run_0.itf.json")
                val script = "echo '{\"states\": [{\"mbt::actionTaken\": \"init\", \"n\": 5}]}' > '$itfFile'\n" +
                    "echo 'error: Invariant violated' >&2\nexit 1"
                return listOf("sh", "-c", script)
            }
        }

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("Violating trace:") == true)
        assertTrue(exception.message?.contains("mbt::actionTaken: \"init\"") == true)
        assertTrue(exception.message?.contains("n: 5") == true)
    }

    @Test
    fun `a non-invariant failure keeps the generic non-zero exit code message`() {
        val script = "echo 'Some other quint error' >&2; exit 1"
        val config = SimpleTestConfig(script, invariants = listOf("safe"))

        val exception = assertThrows(IllegalStateException::class.java) {
            TraceGenerator.generate(config)
        }

        assertTrue(exception.message?.contains("non-zero exit code") == true)
        assertTrue(exception.message?.contains("Some other quint error") == true)
    }

    @Test
    fun `traces stay in numeric sequence order past 9 samples`() {
        val config = object : GeneratorConfig {
            override val seed = "test-seed"
            override val nTraces = 0
            override fun toCommand(tmpDir: Path): List<String> {
                val writes = (1..11).joinToString("\n") { seq ->
                    "echo '{\"states\": [{\"x\": $seq}]}' > '${tmpDir.resolve("run_$seq.itf.json")}'"
                }
                return listOf("sh", "-c", writes)
            }
        }

        val traces = TraceGenerator.generate(config)

        val xValues = traces.map { (it.states.first().value["x"] as ItfValue.Num).value }
        assertEquals((1L..11L).toList(), xValues)
    }

    private class SimpleTestConfig(
        private val script: String,
        override val timeout: Duration = Duration.INFINITE,
        override val invariants: List<String> = emptyList(),
    ) : GeneratorConfig {
        override val seed: String = "test-seed"
        override val nTraces: Int = 0

        override fun toCommand(tmpDir: Path): List<String> =
            listOf("sh", "-c", script)
    }
}
