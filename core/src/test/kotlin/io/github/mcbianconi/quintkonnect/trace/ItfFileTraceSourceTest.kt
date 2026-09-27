package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ItfFileTraceSourceTest {

    private val fakeConfig = object : GeneratorConfig {
        override val seed = "test-seed"
        override val nTraces = 1
        override fun toCommand(tmpDir: Path): List<String> = emptyList()
    }

    private fun xOf(trace: io.github.mcbianconi.itf.ItfTrace): Long =
        (trace.states.first().value["x"] as ItfValue.Num).value

    @Test
    fun `a single file replays as one trace`(@TempDir dir: Path) {
        val file = dir.resolve("a.itf.json")
        Files.writeString(file, """{"states": [{"x": 1}]}""")

        val traces = ItfFileTraceSource(file).generate(fakeConfig)

        assertEquals(1, traces.size)
        assertEquals(1L, xOf(traces[0]))
    }

    @Test
    fun `a directory replays every file in it, sorted numerically`(@TempDir dir: Path) {
        Files.writeString(dir.resolve("run_2.itf.json"), """{"states": [{"x": 2}]}""")
        Files.writeString(dir.resolve("run_11.itf.json"), """{"states": [{"x": 11}]}""")
        Files.writeString(dir.resolve("run_1.itf.json"), """{"states": [{"x": 1}]}""")

        val traces = ItfFileTraceSource(dir).generate(fakeConfig)

        assertEquals(listOf(1L, 2L, 11L), traces.map { xOf(it) })
    }

    @Test
    fun `sorts by the last run of digits, not the first`(@TempDir dir: Path) {
        // "Foo2Test-trace10" vs "Foo2Test-trace2": a first-match rule would read "2" from both
        // names (from "Foo2Test") and fall back to lexicographic order, putting trace10 before
        // trace2. The last run of digits is the trace number itself.
        Files.writeString(dir.resolve("Foo2Test-trace10.itf.json"), """{"states": [{"x": 10}]}""")
        Files.writeString(dir.resolve("Foo2Test-trace2.itf.json"), """{"states": [{"x": 2}]}""")

        val traces = ItfFileTraceSource(dir).generate(fakeConfig)

        assertEquals(listOf(2L, 10L), traces.map { xOf(it) })
    }

    @Test
    fun `a missing path fails naming the resolved absolute path`(@TempDir dir: Path) {
        val missing = dir.resolve("does-not-exist.itf.json")

        val thrown = assertThrows(IllegalStateException::class.java) {
            ItfFileTraceSource(missing).generate(fakeConfig)
        }

        assertTrue(thrown.message!!.contains(missing.toAbsolutePath().toString()))
    }

    @Test
    fun `defaultTraceSource falls back to TraceGenerator when the property is unset`() {
        assertSame(TraceGenerator, defaultTraceSource(testName = "SomeDriver", replayPath = null, tracesDir = null))
    }

    @Test
    fun `defaultTraceSource replays a file when the replay property is set`(@TempDir dir: Path) {
        val file = dir.resolve("a.itf.json")
        Files.writeString(file, """{"states": [{"x": 5}]}""")

        val traces = defaultTraceSource(testName = "SomeDriver", replayPath = file.toString()).generate(fakeConfig)

        assertEquals(5L, xOf(traces[0]))
    }

    @Test
    fun `defaultTraceSource replays a traces directory when quintkonnect-tracesDir has a subdirectory for testName`(
        @TempDir dir: Path,
    ) {
        val driverDir = Files.createDirectories(dir.resolve("SomeDriver"))
        Files.writeString(driverDir.resolve("run_1.itf.json"), """{"states": [{"x": 7}]}""")

        val source = defaultTraceSource(testName = "SomeDriver", replayPath = null, tracesDir = dir.toString())

        assertEquals(7L, xOf(source.generate(fakeConfig)[0]))
    }

    @Test
    fun `defaultTraceSource falls back to TraceGenerator when tracesDir has no subdirectory for testName`(
        @TempDir dir: Path,
    ) {
        assertSame(
            TraceGenerator,
            defaultTraceSource(testName = "MissingDriver", replayPath = null, tracesDir = dir.toString()),
        )
    }

    @Test
    fun `an explicit -Pquint-replay wins over a matching tracesDir subdirectory`(@TempDir dir: Path) {
        val driverDir = Files.createDirectories(dir.resolve("SomeDriver"))
        Files.writeString(driverDir.resolve("run_1.itf.json"), """{"states": [{"x": 7}]}""")
        val replayFile = dir.resolve("replayed.itf.json")
        Files.writeString(replayFile, """{"states": [{"x": 9}]}""")

        val source = defaultTraceSource(testName = "SomeDriver", replayPath = replayFile.toString(), tracesDir = dir.toString())

        assertEquals(9L, xOf(source.generate(fakeConfig)[0]))
    }
}
