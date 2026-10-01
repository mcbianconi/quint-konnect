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
    fun `applies KSP and wires dependencies without quint for a project that has no drivers`() {
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
                    val dependsOnGenerate = testTask.taskDependencies.getDependencies(testTask)
                        .any { it.name == "generateQuintTraces" }
                    println("kspTest=${'$'}kspTest")
                    println("testImplementation=${'$'}testImplementation")
                    println("dependsOnCheckQuint=${'$'}dependsOnCheckQuint")
                    println("dependsOnGenerateQuintTraces=${'$'}dependsOnGenerate")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintKonnectDiagnostics").build()

        assertTrue(result.output.contains("kspTest=io.github.mcbianconi:quint-konnect-ksp:"))
        assertTrue(result.output.contains("testImplementation=io.github.mcbianconi:quint-konnect-core:"))
        assertTrue(result.output.contains("dependsOnCheckQuint=false"))
        assertTrue(result.output.contains("dependsOnGenerateQuintTraces=false"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":printQuintKonnectDiagnostics")?.outcome)
    }

    @Test
    fun `a driver source depends test on checkQuint and not on generateQuintTraces`() {
        File(projectDir, "src/test/kotlin/SampleDriver.kt").apply {
            parentFile.mkdirs()
            writeText("@QuintRun(spec = \"s.qnt\")\nclass SampleDriver\n")
        }
        buildFile.writeText(diagnosticsBuild())

        val result = runner("printQuintKonnectDiagnostics").build()

        assertTrue(result.output.contains("dependsOnCheckQuint=true"))
        assertTrue(result.output.contains("dependsOnGenerateQuintTraces=false"))
    }

    @Test
    fun `generateTraces opts test into generateQuintTraces`() {
        buildFile.writeText(
            diagnosticsBuild(
                """
                quintKonnect {
                    generateTraces.set(true)
                }
                """.trimIndent(),
            ),
        )

        val result = runner("printQuintKonnectDiagnostics").build()

        assertTrue(result.output.contains("dependsOnCheckQuint=true"))
        assertTrue(result.output.contains("dependsOnGenerateQuintTraces=true"))
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

    @Test
    fun `quint dot properties become quintkonnect system properties on Test tasks`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printTestJvmArgs") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("testJvmArgs=" + test.jvmArgumentProviders.flatMap { it.asArguments() }.joinToString(","))
                }
            }
            """.trimIndent(),
        )

        val result = runner(
            "printTestJvmArgs",
            "-Pquint.maxSamples=500",
            "-Pquint.maxSteps=30",
            "-Pquint.seed=0xdeadbeef",
            "-Pquint.verbose=2",
        ).build()

        assertTrue(result.output.contains("-Dquintkonnect.maxSamples=500"))
        assertTrue(result.output.contains("-Dquintkonnect.maxSteps=30"))
        assertTrue(result.output.contains("-Dquintkonnect.seed=0xdeadbeef"))
        assertTrue(result.output.contains("-Dquintkonnect.verbose=2"))
    }

    @Test
    fun `leaving the quint dot properties unset adds none of their system properties`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printTestJvmArgs") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("testJvmArgs=" + test.jvmArgumentProviders.flatMap { it.asArguments() }.joinToString(","))
                }
            }
            """.trimIndent(),
        )

        val result = runner("printTestJvmArgs").build()

        assertTrue(!result.output.contains("quintkonnect.maxSamples"))
        assertTrue(!result.output.contains("quintkonnect.maxSteps"))
        assertTrue(!result.output.contains("quintkonnect.seed"))
        assertTrue(!result.output.contains("quintkonnect.verbose"))
    }

    @Test
    fun `setting -Pquint-replay maps it to quintkonnect-replay and skips checkQuint`() {
        val traceFile = File(projectDir, "saved.itf.json").apply { writeText("""{"states": []}""") }

        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printQuintKonnectDiagnostics") {
                doLast {
                    val testTask = tasks.named("test").get()
                    val dependsOnCheckQuint = testTask.taskDependencies.getDependencies(testTask)
                        .any { it.name == "checkQuint" }
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("dependsOnCheckQuint=${'$'}dependsOnCheckQuint")
                    println("testJvmArgs=" + test.jvmArgumentProviders.flatMap { it.asArguments() }.joinToString(","))
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintKonnectDiagnostics", "-Pquint.replay=${traceFile.name}").build()

        assertTrue(result.output.contains("dependsOnCheckQuint=false"))
        // Not asserting the exact absolute path: TestKit's project dir may not equal
        // traceFile.absolutePath byte-for-byte (e.g. a macOS /var vs /private/var symlink).
        assertTrue(result.output.contains("-Dquintkonnect.replay="))
        assertTrue(result.output.contains(traceFile.name))
    }

    @Test
    fun `a non-numeric -Pquint-maxSamples fails the build at configuration`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint", "-Pquint.maxSamples=notanumber").buildAndFail()

        assertTrue(result.output.contains("-Pquint.maxSamples must be an integer"))
    }

    @Test
    fun `an out-of-range -Pquint-verbose fails the build`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint", "-Pquint.verbose=5").buildAndFail()

        assertTrue(result.output.contains("-Pquint.verbose must be 0, 1 or 2"))
    }

    @Test
    fun `an out-of-range -Pquint-parallelism fails the build`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.mcbianconi.quint-konnect")
            }
            """.trimIndent(),
        )

        val result = runner("checkQuint", "-Pquint.parallelism=0").buildAndFail()

        assertTrue(result.output.contains("-Pquint.parallelism must be at least 1"))
    }

    @Test
    fun `the plugin configures FULL exceptionFormat and standard streams on Test tasks by default`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printTestLogging") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("exceptionFormat=" + test.testLogging.exceptionFormat)
                    println("showStandardStreams=" + test.testLogging.showStandardStreams)
                    println("events=" + test.testLogging.events)
                }
            }
            """.trimIndent(),
        )

        val result = runner("printTestLogging").build()

        assertTrue(result.output.contains("exceptionFormat=FULL"))
        assertTrue(result.output.contains("showStandardStreams=true"))
        assertTrue(result.output.contains("FAILED"))
    }

    @Test
    fun `quintKonnect configureTestLogging set to false leaves Gradle's own defaults in place`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            quintKonnect {
                configureTestLogging.set(false)
            }

            tasks.register("printTestLogging") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("exceptionFormat=" + test.testLogging.exceptionFormat)
                    println("showStandardStreams=" + test.testLogging.showStandardStreams)
                }
            }
            """.trimIndent(),
        )

        val result = runner("printTestLogging").build()

        assertTrue(result.output.contains("exceptionFormat=SHORT"))
        assertTrue(result.output.contains("showStandardStreams=false"))
    }

    @Test
    fun `a project's own testLogging configuration below the plugins block wins`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.test {
                testLogging {
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
                }
            }

            tasks.register("printTestLogging") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("exceptionFormat=" + test.testLogging.exceptionFormat)
                }
            }
            """.trimIndent(),
        )

        val result = runner("printTestLogging").build()

        assertTrue(result.output.contains("exceptionFormat=SHORT"))
    }

    @Test
    fun `setting -Pquint-parallelism maps it to quintkonnect-parallelism`() {
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }

            tasks.register("printTestJvmArgs") {
                doLast {
                    val test = tasks.named("test", org.gradle.api.tasks.testing.Test::class.java).get()
                    println("testJvmArgs=" + test.jvmArgumentProviders.flatMap { it.asArguments() }.joinToString(","))
                }
            }
            """.trimIndent(),
        )

        val result = runner("printTestJvmArgs", "-Pquint.parallelism=4").build()

        assertTrue(result.output.contains("-Dquintkonnect.parallelism=4"))
    }

    private fun diagnosticsBuild(extra: String = ""): String =
        """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
            id("io.github.mcbianconi.quint-konnect")
        }

        $extra

        tasks.register("printQuintKonnectDiagnostics") {
            doLast {
                val testTask = tasks.named("test").get()
                val dependsOnCheckQuint = testTask.taskDependencies.getDependencies(testTask)
                    .any { it.name == "checkQuint" }
                val dependsOnGenerate = testTask.taskDependencies.getDependencies(testTask)
                    .any { it.name == "generateQuintTraces" }
                println("dependsOnCheckQuint=${'$'}dependsOnCheckQuint")
                println("dependsOnGenerateQuintTraces=${'$'}dependsOnGenerate")
            }
        }
        """.trimIndent()

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
}
