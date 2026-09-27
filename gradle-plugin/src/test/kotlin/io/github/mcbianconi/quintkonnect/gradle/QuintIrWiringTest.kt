package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuintIrWiringTest {

    @Test
    fun `quintIr is not registered without kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)

        assertNull(project.tasks.findByName("quintIr"))
    }

    @Test
    fun `quintIr defaults to every dot qnt file under src test resources and depends on checkQuint`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val spec = File(project.projectDir, "src/test/resources/nested/example.qnt")
        spec.parentFile.mkdirs()
        spec.writeText("module example {}")
        File(project.projectDir, "src/test/resources/not-a-spec.txt").apply { parentFile.mkdirs(); writeText("x") }

        val quintIr = project.tasks.getByName("quintIr") as QuintIrTask
        assertEquals(setOf(spec), quintIr.specs.files)
        assertTrue(quintIr.taskDependencies.getDependencies(quintIr).any { it.name == "checkQuint" })
        assertEquals(
            project.layout.buildDirectory.dir("quint-konnect/ir").get().asFile,
            quintIr.outputDir.get().asFile,
        )
    }

    // Two things this ProjectBuilder-based test can't check, covered by QuintIrFunctionalTest
    // instead:
    //  - the exact irDir value passed to KSP: it's read through a Provider chain rooted at
    //    quintIr's own @OutputDirectory property, and Gradle refuses to resolve that before the
    //    producing task has actually run (same reasoning as QuintKonnectPluginTest's
    //    downloadQuint/checkQuint.quintExecutable tests).
    //  - that the "kspTestKotlin" task depends on quintIr: KSP registers its per-compilation tasks
    //    from the Kotlin Gradle plugin's compilation callbacks, which only fire once the project
    //    actually finishes evaluating (real Gradle execution, not ProjectBuilder).
    //  - `-Pquint.replay` skipping the KSP wiring: ProjectBuilder doesn't go through Gradle's
    //    normal property-loading sequence, so `providers.gradleProperty` can't be driven from a
    //    unit test the way GradleRunner's `-P` arguments can.
}
