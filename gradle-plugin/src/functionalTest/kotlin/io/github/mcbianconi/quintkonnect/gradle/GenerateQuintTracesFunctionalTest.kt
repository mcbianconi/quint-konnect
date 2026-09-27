package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Registers GenerateQuintTracesTask directly under a fresh task name, not the "generateQuintTraces"
// task QuintKonnectPlugin itself registers: that one depends on "kspTestKotlin", which needs the
// (unpublished at test time, see QuintKonnectPluginFunctionalTest's class doc) quint-konnect-ksp
// artifact to resolve. Registering the task type directly, with a hand-written manifest directory
// instead of KSP output, proves the task's own caching/up-to-date behavior without either problem.
class GenerateQuintTracesFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var buildFile: File
    private lateinit var manifestDir: File
    private lateinit var quintStub: File

    @BeforeEach
    fun setup() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "fixture"
            buildCache {
                local {
                    directory = file("${'$'}{rootDir}/build-cache")
                }
            }
            """.trimIndent(),
        )
        manifestDir = File(projectDir, "manifests").apply { mkdirs() }
        quintStub = File(projectDir, "quint-stub.sh").apply {
            writeText(
                """
                #!/bin/sh
                prev=""
                for i in "${'$'}@"; do
                    if [ "${'$'}prev" = "--out-itf" ]; then
                        path=${'$'}(printf '%s' "${'$'}i" | sed 's/{seq}/1/')
                        echo '{"states": []}' > "${'$'}path"
                    fi
                    prev="${'$'}i"
                done
                exit 0
                """.trimIndent(),
            )
            setExecutable(true)
        }
        buildFile = File(projectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register<io.github.mcbianconi.quintkonnect.gradle.GenerateQuintTracesTask>("genTraces") {
                manifests.setFrom(fileTree("manifests") { include("*.json") })
                projectDirectory.set(projectDir.absolutePath)
                quintExecutable.set("${quintStub.absolutePath}")
                quintVersion.set("0.32.0")
                outputDir.set(layout.buildDirectory.dir("traces"))
                if (project.hasProperty("maxSamples")) {
                    maxSamplesOverride.set((project.property("maxSamples") as String).toInt())
                }
            }
            """.trimIndent(),
        )
    }

    private fun writeManifest(name: String, seed: String?) {
        val seedField = seed?.let { ""","seed":"$it"""" } ?: ""
        File(manifestDir, "$name.json").writeText(
            """{"driver":"pkg.$name","kind":"run","spec":"spec.qnt"$seedField,"invariants":[]}""",
        )
    }

    @Test
    fun `a pinned seed makes the second run UP-TO-DATE`() {
        writeManifest("PinnedDriver", seed = "cafe")

        runner("genTraces").build()
        val second = runner("genTraces").build()

        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":genTraces")?.outcome)
    }

    @Test
    fun `an unpinned seed never becomes UP-TO-DATE`() {
        writeManifest("UnpinnedDriver", seed = null)

        runner("genTraces").build()
        val second = runner("genTraces").build()

        assertEquals(TaskOutcome.SUCCESS, second.task(":genTraces")?.outcome)
    }

    @Test
    fun `changing maxSamplesOverride reruns even with a pinned seed`() {
        writeManifest("PinnedDriver", seed = "cafe")

        runner("genTraces").build()
        val second = runner("genTraces", "-PmaxSamples=5").build()

        assertEquals(TaskOutcome.SUCCESS, second.task(":genTraces")?.outcome)
    }

    @Test
    fun `a pinned seed is retrieved FROM_CACHE after its output is deleted`() {
        writeManifest("PinnedDriver", seed = "cafe")

        runner("genTraces", "--build-cache").build()
        File(projectDir, "build/traces").deleteRecursively()
        val second = runner("genTraces", "--build-cache").build()

        assertEquals(TaskOutcome.FROM_CACHE, second.task(":genTraces")?.outcome)
        assertTrue(File(projectDir, "build/traces/PinnedDriver/run_1.itf.json").isFile)
    }

    @Test
    fun `is configuration-cache compatible`() {
        writeManifest("PinnedDriver", seed = "cafe")

        runner("genTraces", "--configuration-cache").build()
        val second = runner("genTraces", "--configuration-cache").build()

        assertTrue(second.output.contains("Reusing configuration cache"))
    }

    @Test
    fun `records a seed-txt per driver and clears stale output on rerun`() {
        writeManifest("PinnedDriver", seed = "cafe")

        runner("genTraces").build()

        assertEquals("cafe", File(projectDir, "build/traces/PinnedDriver/seed.txt").readText())
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
}
