package io.github.mcbianconi.quintkonnect.integrationtests.replay

import io.github.mcbianconi.quintkonnect.ReplayRunner
import io.github.mcbianconi.quintkonnect.trace.ItfFileTraceSource
import io.github.mcbianconi.quintkonnect.trace.TestConfig
import org.junit.jupiter.api.Test
import java.nio.file.Path

// Doesn't go through the quintkonnect Gradle plugin's `-Pquint.replay` (this module wires KSP/core
// by hand, see AGENTS.md), so this constructs ItfFileTraceSource directly instead of relying on
// ReplayRunner's default trace source: quint never runs for this test.
class ReplayFixtureTest {

    @Test
    fun `replays a saved trace fixture without invoking quint`() {
        val config = TestConfig(spec = "unused", test = "unused")
        val traceSource = ItfFileTraceSource(Path.of("src/test/resources/replay/counter-trace.itf.json"))

        val replays = ReplayRunner(config, traceSource).traceReplays({ CounterDriver() }, "ReplayFixtureTest")
        replays.forEach { it.run() }
    }
}
