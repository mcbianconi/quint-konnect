package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.trace.TestConfig
import io.github.mcbianconi.quintkonnect.trace.escapeRegex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestConfigTest {

    private val tmpDir = Path.of("tmpdir")

    @Test
    fun `escapeRegex leaves plain identifiers untouched`() {
        assertEquals("happyTest", escapeRegex("happyTest"))
    }

    @Test
    fun `escapeRegex escapes every ECMAScript syntax character`() {
        val syntaxCharacters = "^\$\\.*+?()[]{}|"
        for (c in syntaxCharacters) {
            assertEquals("\\$c", escapeRegex(c.toString()))
        }
    }

    @Test
    fun `escapeRegex leaves non-syntax characters untouched`() {
        assertEquals("a-b_c#d", escapeRegex("a-b_c#d"))
    }

    @Test
    fun `escaped pattern matches the literal name under JVM regex semantics`() {
        val name = "weird.name\$with(chars)"
        val pattern = Regex("^${escapeRegex(name)}$")
        assertTrue(pattern.matches(name))
        assertFalse(pattern.matches("weirdXnameXwith0chars1"))
    }

    @Test
    fun `TestConfig escapes a dollar sign in the test name`() {
        val config = TestConfig(spec = "foo.qnt", test = "test\$foo", seed = "42")
        assertEquals(
            listOf(
                "quint", "test", "foo.qnt",
                "--seed", "42",
                "--match", "^test\\\$foo$",
                "--max-samples", "100",
                "--out-itf", "tmpdir/test_{seq}.itf.json",
                "--verbosity", "0",
            ),
            config.toCommand(tmpDir),
        )
    }

    @Test
    fun `TestConfig escapes regex metacharacters in the test name`() {
        val config = TestConfig(spec = "foo.qnt", test = "my.test(name)", seed = "42")
        assertEquals(
            listOf(
                "quint", "test", "foo.qnt",
                "--seed", "42",
                "--match", "^my\\.test\\(name\\)$",
                "--max-samples", "100",
                "--out-itf", "tmpdir/test_{seq}.itf.json",
                "--verbosity", "0",
            ),
            config.toCommand(tmpDir),
        )
    }
}
