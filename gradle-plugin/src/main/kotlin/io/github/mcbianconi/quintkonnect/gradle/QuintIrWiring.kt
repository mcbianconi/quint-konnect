package io.github.mcbianconi.quintkonnect.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

// Registers `quintIr` and, when `quintKonnect.readSpecIr` is true and not `skipKsp` (replaying a
// saved trace needs no quint install, mirroring QuintKonnectPlugin.kt's `replayOverride == null`
// guard on Test tasks), wires its output directory into KSP as the "quintkonnect.irDir" processor
// option and makes every KSP task depend on it. An empty option value means "no IR".
internal fun wireQuintIr(
    project: Project,
    extension: QuintKonnectExtension,
    checkQuint: TaskProvider<CheckQuintTask>,
    downloadQuint: TaskProvider<DownloadQuintTask>,
    quintExecutablePath: Provider<String>,
    skipKsp: Boolean,
): TaskProvider<QuintIrTask> {
    val quintIr = project.tasks.register("quintIr", QuintIrTask::class.java) { task ->
        task.specs.from(extension.quintIrSpecs)
        task.projectDirectory.set(project.projectDir.absolutePath)
        task.quintExecutable.set(quintExecutablePath)
        task.outputDir.set(project.layout.buildDirectory.dir("quint-konnect/ir"))
        task.dependsOn(checkQuint)
        task.dependsOn(extension.downloadQuint.map { enabled -> if (enabled) listOf(downloadQuint) else emptyList<Any>() })
    }

    if (!skipKsp) {
        project.extensions.getByType(KspExtension::class.java).arg(
            QUINT_IR_DIR_OPTION,
            extension.readSpecIr.flatMap { enabled ->
                if (enabled) quintIr.flatMap { it.outputDir }.map { it.asFile.absolutePath } else project.provider { "" }
            },
        )
        // KSP2 registers one task per Kotlin compilation it processes (e.g. "kspTestKotlin");
        // matching by name prefix avoids depending on its internal task type, which has changed
        // across KSP releases.
        project.tasks.matching { it.name.startsWith("ksp") }.configureEach { task ->
            task.dependsOn(extension.readSpecIr.map { enabled -> if (enabled) listOf(quintIr) else emptyList<Any>() })
        }
    }

    return quintIr
}
