plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("io.github.mcbianconi.quint-konnect")
}

kotlin {
    jvmToolchain(21)
}

quintKonnect {
    // Generates `<Module>Spec` types from each spec under src/test/resources (README.md's
    // "Implement state checking").
    readSpecIr.set(true)
}

dependencies {
    // Only the `suspending` and `asyncstore` drivers need this, for their suspend @QuintActions.
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
