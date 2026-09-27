package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

// Mirrors PROJECT_DIR_PROPERTY in core/.../trace/GeneratorConfig.kt: RunConfig/TestConfig resolve
// a relative `spec` against this system property when a Test task sets it (internal visibility
// is per-module, so this can't just reference core's constant).
internal const val PROJECT_DIR_SYSTEM_PROPERTY: String = "quintkonnect.projectDir"

// Mirrors FAILURES_DIR_PROPERTY in core/.../trace/FailureTraceWriter.kt: a failing trace is saved
// under here by default (internal visibility is per-module, so this can't just reference core's
// constant). Always set, like PROJECT_DIR_SYSTEM_PROPERTY, not a `-Pquint.*` override.
internal const val FAILURES_DIR_SYSTEM_PROPERTY: String = "quintkonnect.failuresDir"

// Mirrors TEST_TASK_PATH_PROPERTY in core/.../trace/FailureTraceWriter.kt: replayCommand() prints
// a `./gradlew <this task's path> --tests ...` command targeting the right task in a
// multi-project build. Always set, like PROJECT_DIR_SYSTEM_PROPERTY.
internal const val TEST_TASK_PATH_SYSTEM_PROPERTY: String = "quintkonnect.testTaskPath"

internal const val KSP_PLUGIN_ID: String = "com.google.devtools.ksp"
internal const val KOTLIN_JVM_PLUGIN_ID: String = "org.jetbrains.kotlin.jvm"

public class QuintKonnectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("quintKonnect", QuintKonnectExtension::class.java)
        extension.quintVersion.convention(DEFAULT_QUINT_VERSION)
        extension.downloadQuint.convention(false)

        // Platform detection only runs if downloadQuint's task configuration is actually realized
        // (i.e. downloadQuint is enabled, or the task is run directly): applying this plugin with
        // downloadQuint left at its default `false` never fails on an unsupported OS/arch.
        val platform: Provider<QuintPlatform> = project.provider { detectQuintPlatform() }

        val downloadQuint = project.tasks.register("downloadQuint", DownloadQuintTask::class.java) { task ->
            task.version.set(extension.quintVersion)
            task.downloadUrl.convention(
                platform.flatMap { p ->
                    extension.quintVersion.map { v ->
                        "https://github.com/quint-co/quint/releases/download/v$v/${p.assetName}"
                    }
                },
            )
            task.expectedSha256.set(
                platform.flatMap { p -> extension.quintVersion.map { v -> knownQuintSha256(v, p.tag) } },
            )
            task.executable.set(
                project.layout.file(
                    platform.flatMap { p ->
                        extension.quintVersion.map { v ->
                            File(
                                project.gradle.gradleUserHomeDir,
                                "caches/quint-konnect/quint/$v/${p.tag}/quint",
                            )
                        }
                    },
                ),
            )
        }

        val checkQuint = project.tasks.register("checkQuint", CheckQuintTask::class.java) { task ->
            task.expectedVersion.set(extension.quintVersion)
            task.quintExecutable.set(quintExecutablePath(project, extension, downloadQuint))
            task.dependsOn(extension.downloadQuint.map { enabled -> if (enabled) listOf(downloadQuint) else emptyList<Any>() })
        }

        // Read once, at configuration time (config-cache safe: providers.gradleProperty), so a
        // malformed -P value fails the build immediately instead of only once a Test task runs.
        val maxSamplesOverride = intGradleProperty(project, MAX_SAMPLES_GRADLE_PROPERTY)
        val maxStepsOverride = intGradleProperty(project, MAX_STEPS_GRADLE_PROPERTY)
        val seedOverride = project.providers.gradleProperty(SEED_GRADLE_PROPERTY).orNull
        val verboseOverride = verboseGradleProperty(project)
        // Resolved against the project directory here (not left to core), so the printed
        // -Pquint.replay path in the plugin-set failure directory and the one the user typed
        // agree regardless of the working directory `gradle` was invoked from.
        val replayOverride = project.providers.gradleProperty(REPLAY_GRADLE_PROPERTY).orNull
            ?.let { project.file(it).absolutePath }
        val parallelismOverride = parallelismGradleProperty(project)
        val replayFiles = replayOverride?.let { project.files(it) } ?: project.files()

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
            val failuresDir = project.layout.buildDirectory.dir("quint-konnect/failures")
            project.tasks.withType(Test::class.java).configureEach { test ->
                // Replaying a saved trace needs no quint installation at all: skip checkQuint (and
                // the download it can depend on) rather than fail a run that never invokes quint.
                if (replayOverride == null) {
                    test.dependsOn(checkQuint)
                    test.dependsOn(
                        extension.downloadQuint.map { enabled -> if (enabled) listOf(downloadQuint) else emptyList<Any>() },
                    )
                }
                test.useJUnitPlatform()
                test.systemProperty(PROJECT_DIR_SYSTEM_PROPERTY, projectDir)
                test.systemProperty(FAILURES_DIR_SYSTEM_PROPERTY, failuresDir.get().asFile.absolutePath)
                test.systemProperty(TEST_TASK_PATH_SYSTEM_PROPERTY, test.path)
                test.jvmArgumentProviders.add(
                    QuintRuntimeArgumentProvider(
                        downloadQuint = extension.downloadQuint,
                        quintExecutablePath = quintExecutablePath(project, extension, downloadQuint),
                        maxSamples = project.provider { maxSamplesOverride },
                        maxSteps = project.provider { maxStepsOverride },
                        seed = project.provider { seedOverride },
                        verbose = project.provider { verboseOverride },
                        replay = project.provider { replayOverride },
                        replayFiles = replayFiles,
                        parallelism = project.provider { parallelismOverride },
                    ),
                )
            }
        }
    }
}

// downloadQuint's output path when enabled, else plain "quint" resolved from PATH as before.
// Always present (never an absent Provider): CheckQuintTask.quintExecutable has its own "quint"
// convention, but Property.set() with an absent Provider would permanently mask that convention
// instead of falling back to it.
private fun quintExecutablePath(
    project: Project,
    extension: QuintKonnectExtension,
    downloadQuint: TaskProvider<DownloadQuintTask>,
): Provider<String> =
    extension.downloadQuint.flatMap { enabled ->
        if (enabled) {
            downloadQuint.flatMap { it.executable }.map { it.asFile.absolutePath }
        } else {
            project.provider { "quint" }
        }
    }

private fun intGradleProperty(project: Project, name: String): Int? {
    val raw = project.providers.gradleProperty(name).orNull ?: return null
    return raw.toIntOrNull() ?: throw GradleException("-P$name must be an integer, got \"$raw\".")
}

private fun verboseGradleProperty(project: Project): Int? {
    val value = intGradleProperty(project, VERBOSE_GRADLE_PROPERTY) ?: return null
    if (value !in 0..2) {
        throw GradleException("-P$VERBOSE_GRADLE_PROPERTY must be 0, 1 or 2, got $value.")
    }
    return value
}

private fun parallelismGradleProperty(project: Project): Int? {
    val value = intGradleProperty(project, PARALLELISM_GRADLE_PROPERTY) ?: return null
    if (value < 1) {
        throw GradleException("-P$PARALLELISM_GRADLE_PROPERTY must be at least 1, got $value.")
    }
    return value
}
