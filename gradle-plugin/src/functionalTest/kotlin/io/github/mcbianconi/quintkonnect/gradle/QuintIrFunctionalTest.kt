package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Runs real quint (unlike QuintKonnectPluginTest/QuintIrTaskTest, which use ProjectBuilder and a
// stub script): skips gracefully instead of failing when quint isn't on PATH, since this is the
// only place quintIr's wiring can be checked against the real `quint typecheck` command.
class QuintIrFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var buildFile: File

    private val fixtureKotlinVersion = System.getProperty("quintkonnect.fixtureKotlinVersion")
        ?: error("quintkonnect.fixtureKotlinVersion system property not set (see gradle-plugin/build.gradle.kts)")

    private fun quintOnPath(): Boolean = try {
        ProcessBuilder("quint", "--version").start().waitFor() == 0
    } catch (e: Exception) {
        false
    }

    @BeforeEach
    fun setup() {
        assumeTrue(quintOnPath(), "quint not found on PATH")
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"fixture\"\n")
        buildFile = File(projectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$fixtureKotlinVersion"
                id("io.github.mcbianconi.quint-konnect")
            }
            """.trimIndent(),
        )
        File(projectDir, "src/test/resources").mkdirs()
        File(projectDir, "src/test/resources/example.qnt").writeText("module example {\n  var x: int\n}\n")
    }

    @Test
    fun `quintIr runs quint typecheck and writes IR named by the spec's relative path`() {
        val result = runner(":quintIr").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":quintIr")?.outcome)
        val irFile = File(projectDir, "build/quint-konnect/ir/src/test/resources/example.qnt.json")
        assertTrue(irFile.isFile)
        assertTrue(irFile.readText().contains("\"errors\":[]"))
    }

    @Test
    fun `the KSP test task depends on quintIr when readSpecIr is set`() {
        buildFile.appendText(
            """

            quintKonnect { readSpecIr.set(true) }

            tasks.register("printQuintIrDependency") {
                doLast {
                    val kspTask = tasks.named("kspTestKotlin").get()
                    val dependsOnQuintIr = kspTask.taskDependencies.getDependencies(kspTask)
                        .any { it.name == "quintIr" }
                    println("dependsOnQuintIr=${'$'}dependsOnQuintIr")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintIrDependency").build()

        assertTrue(result.output.contains("dependsOnQuintIr=true"))
    }

    @Test
    fun `the KSP test task takes quintIr's output as an input when readSpecIr is set`() {
        buildFile.appendText(
            """

            quintKonnect { readSpecIr.set(true) }

            tasks.register("printQuintIrInput") {
                doLast {
                    val kspTask = tasks.named("kspTestKotlin").get()
                    val inputFromQuintIr = kspTask.inputs.files.buildDependencies.getDependencies(kspTask)
                        .any { it.name == "quintIr" }
                    println("inputFromQuintIr=${'$'}inputFromQuintIr")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintIrInput").build()

        assertTrue(result.output.contains("inputFromQuintIr=true"), result.output)
    }

    // Not asserted here: the exact value passed to KSP's "quintkonnect.irDir" option. Referencing
    // com.google.devtools.ksp.gradle.KspExtension from the fixture script resolves to a KspExtension
    // class loaded by a different classloader than the one QuintKonnectPlugin registered the
    // extension under (TestKit's plugin-classpath injection isn't the same as a portal-resolved
    // `plugins {}` application), so `extensions.getByType(KspExtension::class.java)` fails there
    // with "Extension of type 'KspExtension' does not exist" even though it's genuinely registered.
    // "the KSP test task depends on quintIr" above and QuintIrOptionTest (ksp module, which sets
    // the same option name by its constant) cover the wiring instead.

    @Test
    fun `the KSP test task doesn't depend on quintIr by default`() {
        buildFile.appendText(
            """

            tasks.register("printQuintIrDependency") {
                doLast {
                    val kspTask = tasks.named("kspTestKotlin").get()
                    val dependsOnQuintIr = kspTask.taskDependencies.getDependencies(kspTask)
                        .any { it.name == "quintIr" }
                    println("dependsOnQuintIr=${'$'}dependsOnQuintIr")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintIrDependency").build()

        assertTrue(result.output.contains("dependsOnQuintIr=false"))
    }

    @Test
    fun `setting -Pquint-replay does not skip wiring quintIr into KSP when readSpecIr is set`() {
        // qk-sa74: KSP's generated <Module>Spec types are a compile-time dependency of the driver
        // code, not something a replay run can skip the way it skips actually invoking quint at
        // test time (see docs/decisions/replay-needs-quint-with-read-spec-ir.md). Previously,
        // skipping this wiring left KSP without the IR option it had run with before, so it reran
        // and generated no <Module>Spec types, breaking compilation for any driver using them.
        val traceFile = File(projectDir, "saved.itf.json").apply { writeText("""{"states": []}""") }
        buildFile.appendText(
            """

            quintKonnect { readSpecIr.set(true) }

            tasks.register("printQuintIrDependency") {
                doLast {
                    val kspTask = tasks.named("kspTestKotlin").get()
                    val dependsOnQuintIr = kspTask.taskDependencies.getDependencies(kspTask)
                        .any { it.name == "quintIr" }
                    println("dependsOnQuintIr=${'$'}dependsOnQuintIr")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printQuintIrDependency", "-Pquint.replay=${traceFile.name}").build()

        assertTrue(result.output.contains("dependsOnQuintIr=true"))
    }

    @Test
    fun `setting -Pquint-replay still requires quint via checkQuint when readSpecIr is set`() {
        // The flip side of the above: readSpecIr's compile-time dependency on quintIr means a
        // replay run still needs quint installed to (re)generate the IR, even though the replayed
        // Test task itself never invokes quint.
        val traceFile = File(projectDir, "saved.itf.json").apply { writeText("""{"states": []}""") }
        buildFile.appendText(
            """

            quintKonnect { readSpecIr.set(true) }

            tasks.register("printCheckQuintDependency") {
                doLast {
                    val kspTask = tasks.named("kspTestKotlin").get()
                    val dependsOnCheckQuint = kspTask.taskDependencies.getDependencies(kspTask)
                        .flatMap { it.taskDependencies.getDependencies(it) }
                        .any { it.name == "checkQuint" }
                    println("dependsOnCheckQuint=${'$'}dependsOnCheckQuint")
                }
            }
            """.trimIndent(),
        )

        val result = runner("printCheckQuintDependency", "-Pquint.replay=${traceFile.name}").build()

        assertTrue(result.output.contains("dependsOnCheckQuint=true"))
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
}
