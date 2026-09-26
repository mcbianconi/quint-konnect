package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// Mirrors PROJECT_DIR_PROPERTY in core/.../trace/GeneratorConfig.kt: RunConfig/TestConfig resolve
// a relative `spec` against this system property when a Test task sets it (internal visibility
// is per-module, so this can't just reference core's constant).
internal const val PROJECT_DIR_SYSTEM_PROPERTY: String = "quintkonnect.projectDir"

internal const val KSP_PLUGIN_ID: String = "com.google.devtools.ksp"
internal const val KOTLIN_JVM_PLUGIN_ID: String = "org.jetbrains.kotlin.jvm"

public class QuintKonnectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("quintKonnect", QuintKonnectExtension::class.java)
        extension.quintVersion.convention(DEFAULT_QUINT_VERSION)

        val checkQuint = project.tasks.register("checkQuint", CheckQuintTask::class.java) { task ->
            task.expectedVersion.set(extension.quintVersion)
        }

        // React to kotlin.jvm rather than applying it ourselves: build-logic/build.gradle.kts
        // documents why KSP and kotlin.jvm must resolve from the same classpath/classloader,
        // which only holds if kotlin.jvm is already applied.
        project.pluginManager.withPlugin(KOTLIN_JVM_PLUGIN_ID) {
            project.pluginManager.apply(KSP_PLUGIN_ID)

            // KSP writes generated test sources to build/generated/ksp/test/kotlin but doesn't
            // add that directory to the test source set itself.
            project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets
                .getByName("test")
                .kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/test/kotlin"))

            project.configurations.matching { it.name == "kspTest" }.configureEach { configuration ->
                project.dependencies.add(configuration.name, "io.github.mcbianconi:quint-konnect-ksp:$PLUGIN_VERSION")
            }
            project.configurations.matching { it.name == "testImplementation" }.configureEach { configuration ->
                project.dependencies.add(configuration.name, "io.github.mcbianconi:quint-konnect-core:$PLUGIN_VERSION")
            }

            val projectDir = project.projectDir.absolutePath
            project.tasks.withType(Test::class.java).configureEach { test ->
                test.dependsOn(checkQuint)
                test.useJUnitPlatform()
                test.systemProperty(PROJECT_DIR_SYSTEM_PROPERTY, projectDir)
            }
        }
    }
}
