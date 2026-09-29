package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Exercises GenerateQuintTracesTask.run() directly (a plain method call, no GradleRunner/real quint
// needed, mirroring QuintIrTaskTest.kt), against a stub "quint" shell script standing in for
// `quint run`/`quint test`: it never has to invoke real quint, it only has to behave like it (write
// --out-itf files, read --seed, exit 0/1).
class GenerateQuintTracesTaskTest {

    private fun newTask(projectDir: File): GenerateQuintTracesTask {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return project.tasks.register("generateQuintTracesUnderTest", GenerateQuintTracesTask::class.java).get()
    }

    private fun writeManifest(dir: File, name: String, json: String): File {
        val file = File(dir, "$name.json")
        file.writeText(json)
        return file
    }

    // Captures the argv it's invoked with into argsFile, one line per invocation, and writes an
    // ITF trace file at whatever --out-itf path (with "{seq}" replaced by "1") it was given.
    private fun stubQuint(dir: File, argsFile: File, exitCode: Int = 0): File {
        val script = File(dir, "quint-stub.sh")
        script.writeText(
            """
            #!/bin/sh
            printf '%s ' "${'$'}@" >> "$argsFile"
            printf '\n' >> "$argsFile"
            prev=""
            for i in "${'$'}@"; do
                if [ "${'$'}prev" = "--out-itf" ]; then
                    path=${'$'}(printf '%s' "${'$'}i" | sed 's/{seq}/1/')
                    echo '{"states": []}' > "${'$'}path"
                fi
                prev="${'$'}i"
            done
            exit $exitCode
            """.trimIndent(),
        )
        script.setExecutable(true)
        return script
    }

    private fun runConfiguredTask(tempDir: File, manifestDir: File, outDir: File, quint: File): GenerateQuintTracesTask {
        val task = newTask(tempDir)
        // manifests is a flat file collection, not a directory: matches the real wiring
        // (QuintKonnectPlugin.kt's fileTree over generated/ksp), which expands to individual files.
        task.manifests.from(*(manifestDir.listFiles() ?: emptyArray()))
        task.projectDirectory.set(tempDir.absolutePath)
        task.quintExecutable.set(quint.absolutePath)
        task.quintVersion.set("0.32.0")
        task.outputDir.set(outDir)
        task.run()
        return task
    }

    @Test
    fun `runs quint once per manifest into a subdirectory named after the driver's fully qualified class name`(@TempDir tempDir: File) {
        val manifestDir = File(tempDir, "manifests").apply { mkdirs() }
        writeManifest(
            manifestDir,
            "run",
            """{"driver":"pkg.MyDriver","kind":"run","spec":"spec.qnt","seed":"cafe","invariants":[]}""",
        )
        val argsFile = File(tempDir, "args.txt")
        val outDir = File(tempDir, "out")

        runConfiguredTask(tempDir, manifestDir, outDir, stubQuint(tempDir, argsFile))

        val driverDir = File(outDir, "pkg.MyDriver")
        assertTrue(File(driverDir, "run_1.itf.json").isFile)
        assertEquals("cafe", File(driverDir, "seed.txt").readText())
        val args = argsFile.readText()
        assertTrue(args.contains("run"))
        assertTrue(args.contains("--seed cafe"))
        assertTrue(args.contains("--mbt"))
    }

    @Test
    fun `runs a test-kind manifest with quint test and the escaped --match argument`(@TempDir tempDir: File) {
        val manifestDir = File(tempDir, "manifests").apply { mkdirs() }
        writeManifest(
            manifestDir,
            "test",
            """{"driver":"pkg.MyTestDriver","kind":"test","spec":"spec.qnt","test":"a.Test","seed":"beef","invariants":[]}""",
        )
        val argsFile = File(tempDir, "args.txt")
        val outDir = File(tempDir, "out")

        runConfiguredTask(tempDir, manifestDir, outDir, stubQuint(tempDir, argsFile))

        assertTrue(File(File(outDir, "pkg.MyTestDriver"), "test_1.itf.json").isFile)
        val args = argsFile.readText()
        assertTrue(args.contains("test"))
        assertTrue(args.contains("--match ^a\\.Test$"))
    }

    @Test
    fun `-Pquint-seed style override wins over the manifest's own seed`(@TempDir tempDir: File) {
        val manifestDir = File(tempDir, "manifests").apply { mkdirs() }
        writeManifest(
            manifestDir,
            "run",
            """{"driver":"pkg.MyDriver","kind":"run","spec":"spec.qnt","seed":"cafe","invariants":[]}""",
        )
        val argsFile = File(tempDir, "args.txt")
        val outDir = File(tempDir, "out")

        val task = newTask(tempDir)
        task.manifests.from(*(manifestDir.listFiles() ?: emptyArray()))
        task.projectDirectory.set(tempDir.absolutePath)
        task.quintExecutable.set(stubQuint(tempDir, argsFile).absolutePath)
        task.quintVersion.set("0.32.0")
        task.outputDir.set(outDir)
        task.seedOverride.set("0xoverride")
        task.run()

        assertEquals("0xoverride", File(File(outDir, "pkg.MyDriver"), "seed.txt").readText())
    }

    @Test
    fun `records a non-zero exit as error-txt instead of trace files, without throwing`(@TempDir tempDir: File) {
        val manifestDir = File(tempDir, "manifests").apply { mkdirs() }
        writeManifest(
            manifestDir,
            "run",
            """{"driver":"pkg.FailingDriver","kind":"run","spec":"spec.qnt","seed":"cafe","invariants":["safe"]}""",
        )
        val outDir = File(tempDir, "out")
        val quint = File(tempDir, "quint-fail.sh").apply {
            writeText("#!/bin/sh\necho 'error: Invariant violated' 1>&2\nexit 1\n")
            setExecutable(true)
        }

        // Doesn't throw: a quint failure is a normal per-driver outcome, not a build failure.
        runConfiguredTask(tempDir, manifestDir, outDir, quint)

        val driverDir = File(outDir, "pkg.FailingDriver")
        assertFalse(File(driverDir, "run_1.itf.json").exists())
        val error = File(driverDir, "error.txt").readText()
        assertTrue(error.contains("invariant violated"))
        assertTrue(error.contains("safe"))
        assertTrue(error.contains("cafe"))
    }

    @Test
    fun `clears stale output from a previous run`(@TempDir tempDir: File) {
        val manifestDir = File(tempDir, "manifests").apply { mkdirs() }
        writeManifest(
            manifestDir,
            "run",
            """{"driver":"pkg.MyDriver","kind":"run","spec":"spec.qnt","seed":"cafe","invariants":[]}""",
        )
        val outDir = File(tempDir, "out")
        val staleFile = File(File(outDir, "pkg.MyDriver"), "run_99.itf.json").apply {
            parentFile.mkdirs()
            writeText("stale")
        }
        assertTrue(staleFile.exists())

        runConfiguredTask(tempDir, manifestDir, outDir, stubQuint(tempDir, File(tempDir, "args.txt")))

        assertFalse(staleFile.exists())
    }
}
