package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.tasks.testing.Test as TestTask
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuintKonnectPluginTest {

    @Test
    fun `registers checkQuint and the quintKonnect extension with its default version`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)

        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        assertEquals(DEFAULT_QUINT_VERSION, extension.quintVersion.get())
        assertNotNull(project.tasks.findByName("checkQuint"))
    }

    @Test
    fun `does not apply KSP without kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)

        assertNull(project.tasks.findByName("kspTestKotlin"))
        assertTrue(project.configurations.findByName("kspTest") == null)
    }

    @Test
    fun `applying kotlin jvm wires KSP and quint-konnect dependencies`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        assertTrue(project.pluginManager.hasPlugin(KSP_PLUGIN_ID))

        val kspTestDeps = project.configurations.getByName("kspTest").dependencies
        assertTrue(kspTestDeps.any { it.group == "io.github.mcbianconi" && it.name == "quint-konnect-ksp" })

        val testImplDeps = project.configurations.getByName("testImplementation").dependencies
        assertTrue(testImplDeps.any { it.group == "io.github.mcbianconi" && it.name == "quint-konnect-core" })
    }

    @Test
    fun `applying kotlin jvm sets the projectDir system property and the KSP source dir on test tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val testTask = project.tasks.getByName("test") as TestTask
        assertEquals(project.projectDir.absolutePath, testTask.systemProperties[PROJECT_DIR_SYSTEM_PROPERTY])

        val expectedSrcDir = project.layout.buildDirectory.dir("generated/ksp/test/kotlin").get().asFile
        val testSourceSet = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            .sourceSets.getByName("test")
        assertTrue(testSourceSet.kotlin.srcDirs.contains(expectedSrcDir))
    }
}
