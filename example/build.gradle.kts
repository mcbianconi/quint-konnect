plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

group   = "io.github.mcbianconi.quintkonnect"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)

    kspTest(project(":ksp"))

    testImplementation(project(":core"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
