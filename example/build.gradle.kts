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

dependencies {
    implementation(libs.kotlinx.serialization.json)

    kspTest(project(":ksp"))

    testImplementation(project(":core"))
    testImplementation(libs.kotlinx.serialization.json)
    // Only the `suspending` example driver needs this, for its suspend @QuintAction (qk-33ky).
    testImplementation(libs.kotlinx.coroutines.core)
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
