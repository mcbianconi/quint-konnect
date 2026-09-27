package io.github.mcbianconi.quintkonnect.trace

import io.github.mcbianconi.itf.ItfState
import io.github.mcbianconi.itf.ItfTrace
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.parseTrace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FailureTraceWriterTest {

    private val sampleTrace = ItfTrace(
        vars = listOf("count", "lastAction"),
        states = listOf(
            ItfState(
                linkedMapOf(
                    "count" to ItfValue.Num(0),
                    "flag" to ItfValue.Bool(true),
                    "name" to ItfValue.Str("hi"),
                    "big" to ItfValue.BigInt("12345678901234567890"),
                    "list" to ItfValue.List(listOf(ItfValue.Num(1), ItfValue.Num(2))),
                    "tup" to ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Str("a"))),
                    "set" to ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2))),
                    "map" to ItfValue.Map(listOf(ItfValue.Num(1) to ItfValue.Str("one"))),
                    "rec" to ItfValue.Record(linkedMapOf("x" to ItfValue.Num(1))),
                ),
            ),
        ),
    )

    @Test
    fun `toItfJson round-trips through parseTrace`() {
        val json = sampleTrace.toItfJson()
        val parsed = parseTrace(json)

        assertEquals(sampleTrace, parsed)
    }

    @Test
    fun `writeFailureTrace names the file with a 1-based trace number`(@TempDir dir: Path) {
        val file = writeFailureTrace("MyTest", 0, sampleTrace, dir)

        assertEquals("MyTest-trace1.itf.json", file.fileName.toString())
        assertTrue(Files.exists(file))
        assertEquals(sampleTrace, parseTrace(file.readTextCompat()))
    }

    @Test
    fun `writeFailureTrace replaces a file already there`(@TempDir dir: Path) {
        writeFailureTrace("MyTest", 0, sampleTrace, dir)
        val other = ItfTrace(states = listOf(ItfState(linkedMapOf("x" to ItfValue.Num(9)))))
        val file = writeFailureTrace("MyTest", 0, other, dir)

        assertEquals(other, parseTrace(file.readTextCompat()))
    }

    @Test
    fun `writeFailureTrace creates the directory if missing`(@TempDir dir: Path) {
        val nested = dir.resolve("a/b/c")
        val file = writeFailureTrace("MyTest", 0, sampleTrace, nested)

        assertTrue(Files.exists(file))
    }

    @Test
    fun `resolveFailuresDir prefers the override property`() {
        val dir = resolveFailuresDir(override = "/tmp/somewhere", projectDir = "/tmp/proj")
        assertEquals(Path.of("/tmp/somewhere"), dir)
    }

    @Test
    fun `resolveFailuresDir falls back to build_quint-konnect_failures under the project dir`() {
        val dir = resolveFailuresDir(override = null, projectDir = "/tmp/proj")
        assertEquals(Path.of("/tmp/proj/build/quint-konnect/failures"), dir)
    }

    @Test
    fun `resolveFailuresDir falls back to the working directory when neither is set`() {
        val dir = resolveFailuresDir(override = null, projectDir = null)
        assertEquals(Path.of(".", "build", "quint-konnect", "failures"), dir)
    }

    @Test
    fun `replayCommand relativizes the file against the project dir`() {
        val command = replayCommand(
            testName = "MyDriver",
            file = Path.of("/proj/build/quint-konnect/failures/MyDriver-trace1.itf.json"),
            taskPath = ":test",
            projectDir = "/proj",
        )

        assertEquals(
            "./gradlew :test --tests '*MyDriver*' -Pquint.replay=build/quint-konnect/failures/MyDriver-trace1.itf.json",
            command,
        )
    }

    @Test
    fun `replayCommand falls back to the absolute path and plain test task`() {
        val command = replayCommand(
            testName = "MyDriver",
            file = Path.of("/elsewhere/MyDriver-trace1.itf.json"),
            taskPath = null,
            projectDir = "/proj",
        )

        assertEquals(
            "./gradlew test --tests '*MyDriver*' -Pquint.replay=/elsewhere/MyDriver-trace1.itf.json",
            command,
        )
    }

    private fun Path.readTextCompat(): String = Files.readString(this)
}
