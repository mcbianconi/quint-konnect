package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Exercises QuintIrTask.run() directly (a plain method call, no GradleRunner/real quint needed),
// against a stub "quint" shell script standing in for `quint typecheck --out`: it never has to
// invoke real quint, it only has to behave like it (write the --out file, exit 0/1).
class QuintIrTaskTest {

    private fun newTask(projectDir: File): QuintIrTask {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return project.tasks.register("quintIrUnderTest", QuintIrTask::class.java).get()
    }

    // Writes `{"errors":[...]}` to its --out argument and exits with the given code, mirroring
    // `quint typecheck --out`'s own behavior of suppressing all console output. A single-quoted
    // heredoc delimiter (not `echo`) avoids the shell re-interpreting errorsJson's own quotes.
    private fun stubQuint(dir: File, exitCode: Int, errorsJson: String = "[]"): File {
        val script = File(dir, "quint-stub.sh")
        script.writeText(
            """
            #!/bin/sh
            cat > "${'$'}3" <<'IRJSON'
            {"errors":$errorsJson}
            IRJSON
            exit $exitCode
            """.trimIndent(),
        )
        script.setExecutable(true)
        return script
    }

    @Test
    fun `writes one IR file per spec, named by its path relative to the project directory`(@TempDir tempDir: File) {
        val specDir = File(tempDir, "src/test/resources").apply { mkdirs() }
        val spec = File(specDir, "nested/example.qnt").apply {
            parentFile.mkdirs()
            writeText("module example {}")
        }
        val outDir = File(tempDir, "out")

        val task = newTask(tempDir)
        task.specs.from(spec)
        task.projectDirectory.set(tempDir.absolutePath)
        task.quintExecutable.set(stubQuint(tempDir, exitCode = 0).absolutePath)
        task.outputDir.set(outDir)

        task.run()

        val expected = File(outDir, "src/test/resources/nested/example.qnt.json")
        assertTrue(expected.isFile)
        assertEquals("{\"errors\":[]}", expected.readText().trim())
    }

    @Test
    fun `throws with the IR's error explanations and deletes the partial output on a nonzero exit`(
        @TempDir tempDir: File,
    ) {
        val spec = File(tempDir, "broken.qnt").apply { writeText("module broken {}") }
        val outDir = File(tempDir, "out")

        val task = newTask(tempDir)
        task.specs.from(spec)
        task.projectDirectory.set(tempDir.absolutePath)
        task.quintExecutable.set(
            stubQuint(tempDir, exitCode = 1, errorsJson = "[{\"explanation\":\"boom\"}]").absolutePath,
        )
        task.outputDir.set(outDir)

        val exception = assertThrows(GradleException::class.java) { task.run() }
        assertTrue(exception.message!!.contains("boom"))
        assertFalse(File(outDir, "broken.qnt.json").exists())
    }
}
