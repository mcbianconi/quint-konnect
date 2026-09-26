package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

// Doesn't resolve `io.github.mcbianconi:quint-konnect-core`/`-ksp` (unpublished at test time, and
// publishToMavenLocal isn't allowed in tests): asserts the configured dependency coordinates and
// task wiring instead of compiling or running the fixture's own tests.
class QuintKonnectPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var buildFile: File

    private val fixtureKotlinVersion = System.getProperty("quintkonnect.fixtureKotlinVersion")
        ?: error("quintkonnect.fixtureKotlinVersion system property not set (see gradle-plugin/build.gradle.kts)")

    @BeforeEach
    fun setup() {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"fixture\"\n")
        buildFile = File(projectDir, "build.gradle.kts")
    }

    @Test
    fun `applies KSP, wires dependencies and depends every Test task on checkQuint`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printQuintKonnectDiagnostics") {
                doLast {
                    val kspTest = configurations.getByName("kspTest").dependencies
                        .joinToString { "${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}" }
                    val testImplementation = configurations.getByName("testImplementation").dependencies
                        .joinToString { "${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}" }
                    val testTask = tasks.named("test").get()
                    val dependsOnCheckQuint = testTask.taskDependencies.getDependencies(testTask)
                        .any { it.name == "checkQuint" }
                    println("kspTest=${'$'}kspTest")
                    println("testImplementation=${'$'}testImplementation")
                    println("dependsOnCheckQuint=${'$'}dependsOnCheckQuint")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintKonnectDiagnostics").build()

        assertTrue(result.output.contains("kspTest=io.github.mcbianconi:quint-konnect-ksp:"))
        assertTrue(result.output.contains("testImplementation=io.github.mcbianconi:quint-konnect-core:"))
        assertTrue(result.output.contains("dependsOnCheckQuint=true"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":printQuintKonnectDiagnostics")?.outcome)
    }

    @Test
    fun `checkQuint is registered even without kotlin jvm applied`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printCheckQuint") {
                doLast {
                    println("checkQuintRegistered=${'$'}{tasks.findByName("checkQuint") != null}")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printCheckQuint").build()

        assertTrue(result.output.contains("checkQuintRegistered=true"))
    }

    @Test
    fun `checkQuint fails with install instructions when quint is not found`() {
        // Points checkQuint at a nonexistent executable name rather than hiding the real quint
        // from PATH: GradleRunner's Tooling API connection always goes through a (possibly
        // reused) daemon, whose child processes inherit its own real OS environment regardless
        // of withEnvironment(), and --no-daemon isn't a supported Tooling API build argument.
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.withType<io.github.mcbianconi.quintkonnect.gradle.CheckQuintTask>().configureEach {
                quintExecutable.set("quint-konnect-test-nonexistent-executable")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint").buildAndFail()

        assertTrue(result.output.contains("$QUINT_INSTALL_COMMAND@$DEFAULT_QUINT_VERSION"))
    }

    @Test
    fun `checkQuint is configuration-cache compatible`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }
            """.trimIndent(),
        )

        runner("checkQuint", "--configuration-cache").build()
        val second = runner("checkQuint", "--configuration-cache").build()

        assertTrue(second.output.contains("Reusing configuration cache"))
    }

    @Test
    fun `checkQuint warns on a version mismatch instead of failing`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            quintKonnect {
                quintVersion.set("0.0.0-does-not-exist")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkQuint")?.outcome)
        assertTrue(result.output.contains("but this project is configured for 0.0.0-does-not-exist"))
    }

    @Test
    fun `downloadQuint fetches a file fixture, verifies it and checkQuint uses it`() {
        // Stands in for a GitHub release asset: a real (shell-script) executable so checkQuint's
        // `quintExecutable --version` exec actually runs it, no network involved.
        val fixtureVersion = "0.0.0-download-e2e"
        val fixture = File(projectDir, "fixture-quint").apply {
            writeText("#!/bin/sh\necho $fixtureVersion\n")
        }
        val sha256 = MessageDigest.getInstance("SHA-256").digest(fixture.readBytes())
            .joinToString("") { "%02x".format(it) }

        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            quintKonnect {
                quintVersion.set("$fixtureVersion")
                downloadQuint.set(true)
            }

            tasks.withType<io.github.mcbianconi.quintkonnect.gradle.DownloadQuintTask>().configureEach {
                downloadUrl.set("${fixture.toURI()}")
                expectedSha256.set("$sha256")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":downloadQuint")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkQuint")?.outcome)
        assertTrue(!result.output.contains("but this project is configured for"))
    }

    @Test
    fun `downloadQuint fails checkQuint's build when the checksum does not match`() {
        val fixture = File(projectDir, "fixture-quint").apply {
            writeText("#!/bin/sh\necho mismatched\n")
        }

        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }

            quintKonnect {
                quintVersion.set("0.0.0-download-mismatch")
                downloadQuint.set(true)
            }

            tasks.withType<io.github.mcbianconi.quintkonnect.gradle.DownloadQuintTask>().configureEach {
                downloadUrl.set("${fixture.toURI()}")
                expectedSha256.set("0000000000000000000000000000000000000000000000000000000000000000")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint").buildAndFail()

        assertTrue(result.output.contains("checksum verification"))
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
}
