package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.tasks.testing.Test as TestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `downloadQuint defaults to false and checkQuint keeps resolving quint from PATH`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)

        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        assertFalse(extension.downloadQuint.get())

        val checkQuint = project.tasks.getByName("checkQuint") as CheckQuintTask
        assertEquals("quint", checkQuint.quintExecutable.get())
        assertTrue(checkQuint.taskDependencies.getDependencies(checkQuint).none { it.name == "downloadQuint" })
    }

    @Test
    fun `registers downloadQuint with the pinned version's cache path`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)

        val downloadQuint = project.tasks.getByName("downloadQuint") as DownloadQuintTask
        assertEquals(DEFAULT_QUINT_VERSION, downloadQuint.version.get())

        val expectedPrefix = project.gradle.gradleUserHomeDir.resolve("caches/quint-konnect/quint/$DEFAULT_QUINT_VERSION")
        assertTrue(downloadQuint.executable.get().asFile.absolutePath.startsWith(expectedPrefix.absolutePath))
        assertTrue(downloadQuint.executable.get().asFile.name == "quint")
    }

    @Test
    fun `enabling downloadQuint points checkQuint at the downloaded executable and depends on it`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        extension.downloadQuint.set(true)

        // checkQuint.quintExecutable resolves to downloadQuint's output only once downloadQuint
        // has actually run (Gradle refuses to query a task-output-derived Provider before that:
        // see the E2E functionalTest for the resolved value). Here just assert the dependency.
        val checkQuint = project.tasks.getByName("checkQuint") as CheckQuintTask
        assertTrue(checkQuint.taskDependencies.getDependencies(checkQuint).any { it.name == "downloadQuint" })
    }

    @Test
    fun `enabling downloadQuint makes Test tasks depend on downloadQuint`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        extension.downloadQuint.set(true)

        val testTask = project.tasks.getByName("test") as TestTask
        assertTrue(testTask.taskDependencies.getDependencies(testTask).any { it.name == "downloadQuint" })
    }

    @Test
    fun `leaving downloadQuint disabled does not depend on downloadQuint`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val testTask = project.tasks.getByName("test") as TestTask
        assertTrue(testTask.taskDependencies.getDependencies(testTask).none { it.name == "downloadQuint" })

        val checkQuint = project.tasks.getByName("checkQuint") as CheckQuintTask
        assertEquals("quint", checkQuint.quintExecutable.get())
    }

    @Test
    fun `configureTestLogging defaults to true and configures FULL, standard streams and FAILED events`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        assertTrue(extension.configureTestLogging.get())

        val testTask = project.tasks.getByName("test") as TestTask
        assertEquals(TestExceptionFormat.FULL, testTask.testLogging.exceptionFormat)
        assertTrue(testTask.testLogging.showStandardStreams)
        assertTrue(testTask.testLogging.events.contains(TestLogEvent.FAILED))
    }

    @Test
    fun `configureTestLogging set to false leaves Gradle's own testLogging defaults in place`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        val extension = project.extensions.getByType(QuintKonnectExtension::class.java)
        extension.configureTestLogging.set(false)

        val testTask = project.tasks.getByName("test") as TestTask
        assertEquals(TestExceptionFormat.SHORT, testTask.testLogging.exceptionFormat)
        assertFalse(testTask.testLogging.showStandardStreams)
    }

    @Test
    fun `a project's own testLogging configuration applied after the plugin wins`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(QuintKonnectPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.tasks.withType(TestTask::class.java).configureEach { test ->
            test.testLogging { it.exceptionFormat = TestExceptionFormat.SHORT }
        }

        val testTask = project.tasks.getByName("test") as TestTask
        assertEquals(TestExceptionFormat.SHORT, testTask.testLogging.exceptionFormat)
    }
}
