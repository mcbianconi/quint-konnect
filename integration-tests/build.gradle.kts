// Unpublished regression tests against real quint that reach into core's internals (ReplayRunner,
// ItfFileTraceSource, hand-set quintkonnect.* properties) or fixtures no user would write (escaping/),
// so they don't belong in example/. Wires KSP/core as project dependencies, not through the Gradle
// plugin, so running them needs no publishToMavenLocal.
plugins {
    id("quintkonnect.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
    id("quintkonnect.ksp")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

// Mirrors the Gradle plugin's quintIr task (gradle-plugin/.../QuintIrTask.kt), since this module
// wires KSP by hand: each IR file must be "<irDir>/<spec as @QuintRun/@QuintTest names it>.json".
// escaping/ is left out: EscapingCounterDriver declares an action the spec doesn't have on purpose,
// which the spec IR check (qk-75ad) would reject.
val quintIrDir = layout.buildDirectory.dir("quint-konnect/ir")
val quintIr = tasks.register("quintIr")
fileTree("src/test/resources") { include("**/*.qnt"); exclude("escaping/**") }.forEach { spec ->
    val relativePath = spec.relativeTo(projectDir).path
    val out = quintIrDir.get().file("$relativePath.json").asFile
    val task = tasks.register<Exec>("quintIr_" + relativePath.replace(Regex("[^A-Za-z0-9]"), "_")) {
        inputs.file(spec).withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(out)
        doFirst { out.parentFile.mkdirs() }
        commandLine("quint", "typecheck", "--out", out.absolutePath, spec.absolutePath)
    }
    quintIr.configure { dependsOn(task) }
}

ksp {
    arg("quintkonnect.irDir", quintIrDir.map { it.asFile.absolutePath })
}

tasks.matching { it.name == "kspTestKotlin" }.configureEach {
    dependsOn(quintIr)
    // The IR is read through a processor option, not a source file, so without this KSP stays
    // UP-TO-DATE after a spec change and the generated <Module>Spec types go stale.
    inputs.dir(quintIrDir).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("quintIr")
}

dependencies {
    kspTest(project(":ksp"))

    testImplementation(project(":core"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    // Matches PROJECT_DIR_PROPERTY in core/.../trace/GeneratorConfig.kt: without the
    // quintkonnect Gradle plugin (qk-udpu) to set this, wire it by hand so a relative `spec`
    // resolves the same way from `gradle test` and from an IDE run.
    systemProperty("quintkonnect.projectDir", project.projectDir.absolutePath)
}
