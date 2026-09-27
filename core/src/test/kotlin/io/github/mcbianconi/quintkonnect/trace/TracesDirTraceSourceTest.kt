package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TracesDirTraceSourceTest {

    private val fakeConfig = object : GeneratorConfig {
        override val seed = "test-seed"
        override val nTraces = 1
        override fun toCommand(tmpDir: Path): List<String> = emptyList()
    }

    private fun xOf(trace: io.github.mcbianconi.itf.ItfTrace): Long =
        (trace.states.first().value["x"] as ItfValue.Num).value

    @Test
    fun `reads only itf-json files, sorted numerically`(@TempDir dir: Path) {
        Files.writeString(dir.resolve("run_2.itf.json"), """{"states": [{"x": 2}]}""")
        Files.writeString(dir.resolve("run_1.itf.json"), """{"states": [{"x": 1}]}""")
        Files.writeString(dir.resolve(SEED_FILE_NAME), "0xabc")

        val traces = TracesDirTraceSource(dir).generate(fakeConfig)

        assertEquals(listOf(1L, 2L), traces.map { xOf(it) })
    }

    @Test
    fun `an empty directory returns zero traces, not an error`(@TempDir dir: Path) {
        assertEquals(emptyList<Any>(), TracesDirTraceSource(dir).generate(fakeConfig))
    }

    @Test
    fun `throws with the recorded error text instead of returning traces when error-txt is present`(@TempDir dir: Path) {
        Files.writeString(dir.resolve("run_1.itf.json"), """{"states": [{"x": 1}]}""")
        Files.writeString(dir.resolve(ERROR_FILE_NAME), "Quint invariant violated: safe (seed 0xabc)")

        val thrown = assertThrows(IllegalStateException::class.java) { TracesDirTraceSource(dir).generate(fakeConfig) }

        assertEquals("Quint invariant violated: safe (seed 0xabc)", thrown.message)
    }

    @Test
    fun `recordedSeed reads seed-txt when present`(@TempDir dir: Path) {
        Files.writeString(dir.resolve(SEED_FILE_NAME), "0xabc\n")
        assertEquals("0xabc", TracesDirTraceSource(dir).recordedSeed())
    }

    @Test
    fun `recordedSeed is null when seed-txt is absent`(@TempDir dir: Path) {
        assertNull(TracesDirTraceSource(dir).recordedSeed())
    }

    @Test
    fun `sorts by the last run of digits, not the first`(@TempDir dir: Path) {
        Files.writeString(dir.resolve("Foo2Test-trace10.itf.json"), """{"states": [{"x": 10}]}""")
        Files.writeString(dir.resolve("Foo2Test-trace2.itf.json"), """{"states": [{"x": 2}]}""")

        val traces = TracesDirTraceSource(dir).generate(fakeConfig)

        assertTrue(traces.map { xOf(it) } == listOf(2L, 10L))
    }
}
