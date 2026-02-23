package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.RunConfig
import io.github.mcbianconi.quintkonnect.trace.TestConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

class RunConfigTest {

    private val tmpDir = Path.of("tmpdir")

    @Test
    fun `RunConfig basic command`() {
        val config = RunConfig(spec = "foo.qnt", seed = "42")
        assertEquals(
            listOf(
                "quint", "run", "foo.qnt",
                "--seed", "42",
                "--max-samples", "100",
                "--n-traces", "100",
                "--out-itf", "tmpdir/run_{seq}.itf.json",
                "--mbt",
                "--verbosity", "0",
            ),
            config.toCommand(tmpDir),
        )
    }

    @Test
    fun `RunConfig with all options`() {
        val config = RunConfig(
            spec = "foo.qnt",
            seed = "42",
            main = "MyModule",
            init = "myInit",
            step = "myStep",
            maxSamples = 50,
            maxSteps = 20,
        )
        assertEquals(
            listOf(
                "quint", "run", "foo.qnt",
                "--seed", "42",
                "--max-samples", "50",
                "--n-traces", "50",
                "--out-itf", "tmpdir/run_{seq}.itf.json",
                "--mbt",
                "--verbosity", "0",
                "--main", "MyModule",
                "--init", "myInit",
                "--step", "myStep",
                "--max-steps", "20",
            ),
            config.toCommand(tmpDir),
        )
    }

    @Test
    fun `TestConfig basic command`() {
        val config = TestConfig(spec = "foo.qnt", test = "happyTest", seed = "42")
        assertEquals(
            listOf(
                "quint", "test", "foo.qnt",
                "--seed", "42",
                "--match", "^happyTest$",
                "--max-samples", "100",
                "--out-itf", "tmpdir/test_{seq}.itf.json",
                "--verbosity", "0",
            ),
            config.toCommand(tmpDir),
        )
    }

    @Test
    fun `TestConfig with main`() {
        val config = TestConfig(spec = "foo.qnt", test = "myTest", main = "tests", seed = "42")
        assertEquals(
            listOf(
                "quint", "test", "foo.qnt",
                "--seed", "42",
                "--match", "^myTest$",
                "--max-samples", "100",
                "--out-itf", "tmpdir/test_{seq}.itf.json",
                "--verbosity", "0",
                "--main", "tests",
            ),
            config.toCommand(tmpDir),
        )
    }
}
