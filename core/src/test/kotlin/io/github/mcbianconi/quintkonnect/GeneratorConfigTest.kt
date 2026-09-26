package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.PROJECT_DIR_PROPERTY
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import io.github.mcbianconi.quintkonnect.trace.resolveSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

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
}
