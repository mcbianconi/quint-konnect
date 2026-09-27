package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
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

// Mirrors SHRINK_PROPERTY in core/.../trace/Shrink.kt: set only on shrinkQuintTraces (qk-a8ay).
internal const val SHRINK_SYSTEM_PROPERTY: String = "quintkonnect.shrink"
internal const val SHRINK_TASK_NAME: String = "shrinkQuintTraces"

internal const val KSP_PLUGIN_ID: String = "com.google.devtools.ksp"
internal const val KOTLIN_JVM_PLUGIN_ID: String = "org.jetbrains.kotlin.jvm"

public class QuintKonnectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("quintKonnect", QuintKonnectExtension::class.java)
        extension.quintVersion.convention(DEFAULT_QUINT_VERSION)
        extension.downloadQuint.convention(false)
        extension.configureTestLogging.convention(true)
        extension.readSpecIr.convention(false)
        extension.quintIrSpecs.from(project.fileTree(project.projectDir) { it.include("src/test/resources/**/*.qnt") })

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

            wireQuintIr(
                project = project,
                extension = extension,
                checkQuint = checkQuint,
                downloadQuint = downloadQuint,
                quintExecutablePath = quintExecutablePath(project, extension, downloadQuint),
            )

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

            // Computed once, independent of generateQuintTraces' own outputDir property (see the
            // longer comment further below on tracesDirFiles/tracesDirPath, and failuresDir's
            // identical shape a few lines down): both the task's own outputDir and the Test tasks'
            // TracesDirArgumentProvider read this same value instead of one deriving from the other.
            val tracesDir = project.layout.buildDirectory.dir("quint-konnect/traces")

            val generateQuintTraces = project.tasks.register("generateQuintTraces", GenerateQuintTracesTask::class.java) { task ->
                task.manifests.setFrom(
                    project.fileTree(project.layout.buildDirectory.dir("generated/ksp")) {
                        it.include("**/quintkonnect/traces-manifest/**/*.json")
                    },
                )
                // "kspTestKotlin" (KSP2 registers one task per Kotlin compilation) by name, not by
                // task reference: it may not be registered yet at this point, and a String task name
                // is resolved lazily against the task container (mirrors wireQuintIr's own comment
                // about not depending on KSP's internal task type, QuintIrWiring.kt).
                task.dependsOn("kspTestKotlin")
                task.projectDirectory.set(project.projectDir.absolutePath)
                task.quintExecutable.set(quintExecutablePath(project, extension, downloadQuint))
                task.quintVersion.set(extension.quintVersion)
                task.maxSamplesOverride.set(project.provider { maxSamplesOverride })
                task.maxStepsOverride.set(project.provider { maxStepsOverride })
                task.seedOverride.set(project.provider { seedOverride })
                task.envSeed.set(project.providers.environmentVariable("QUINT_SEED"))
                task.outputDir.set(tracesDir)
                task.dependsOn(checkQuint)
                task.dependsOn(
                    extension.downloadQuint.map { enabled -> if (enabled) listOf(downloadQuint) else emptyList<Any>() },
                )
            }

            // qk-a8ay: `test`'s own classes, rerun with shrinking on. It runs quint itself instead of
            // replaying generateQuintTraces' output (shrinking needs fresh runs with a smaller
            // --max-steps), so it's the one Test task without a tracesDir; filter it to one driver
            // with --tests and pin the failing run's seed with -Pquint.seed.
            project.tasks.register(SHRINK_TASK_NAME, Test::class.java) { task ->
                val test = project.tasks.named("test", Test::class.java)
                task.group = "verification"
                task.description = "Reruns failing quint-konnect traces with the same seed and a smaller " +
                    "--max-steps, reporting the shortest failing trace found."
                task.testClassesDirs = test.get().testClassesDirs
                task.classpath = test.get().classpath
                task.systemProperty(SHRINK_SYSTEM_PROPERTY, "true")
                task.outputs.upToDateWhen { false }
            }

            val projectDir = project.projectDir.absolutePath
            val failuresDir = project.layout.buildDirectory.dir("quint-konnect/failures")
            project.tasks.withType(Test::class.java).configureEach { test ->
                // Replaying a saved trace needs no quint installation at all: skip checkQuint (and
                // the download it can depend on) rather than fail a run that never invokes quint.
                // generateQuintTraces needs quint too, for the same reason.
                if (replayOverride == null) {
                    test.dependsOn(checkQuint)
                    test.dependsOn(
                        extension.downloadQuint.map { enabled -> if (enabled) listOf(downloadQuint) else emptyList<Any>() },
                    )
                }
                if (replayOverride == null && test.name != SHRINK_TASK_NAME) {
                    test.dependsOn(generateQuintTraces)
                    test.jvmArgumentProviders.add(
                        TracesDirArgumentProvider(
                            tracesDirPath = tracesDir.map { it.asFile.absolutePath },
                            tracesDirFiles = project.files(generateQuintTraces.map { it.outputDir }),
                        ),
                    )
                }
                test.useJUnitPlatform()
                test.systemProperty(PROJECT_DIR_SYSTEM_PROPERTY, projectDir)
                test.systemProperty(FAILURES_DIR_SYSTEM_PROPERTY, failuresDir.get().asFile.absolutePath)
                // A trace saved by shrinkQuintTraces replays through the regular test task.
                val replayTaskPath = if (test.name == SHRINK_TASK_NAME) test.path.substringBeforeLast(':') + ":test" else test.path
                test.systemProperty(TEST_TASK_PATH_SYSTEM_PROPERTY, replayTaskPath)
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

            // Its own configureEach, registered after the one above: Gradle runs configureEach
            // actions in registration order when a Test task is realized, so a project's own
            // testLogging configuration (applied later, e.g. below the plugins {} block in the
            // same build.gradle.kts) still runs after this one and wins.
            project.tasks.withType(Test::class.java).configureEach { test ->
                if (extension.configureTestLogging.get()) {
                    test.testLogging { logging ->
                        logging.exceptionFormat = TestExceptionFormat.FULL
                        logging.showStandardStreams = true
                        logging.events(TestLogEvent.FAILED)
                    }
                }
            }
        }
    }
}

// downloadQuint's output path when enabled, else plain "quint" resolved from PATH as before.
// Always present (never an absent Provider): CheckQuintTask.quintExecutable has its own "quint"
// convention, but Property.set() with an absent Provider would permanently mask that convention
// instead of falling back to it.
internal fun quintExecutablePath(
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
