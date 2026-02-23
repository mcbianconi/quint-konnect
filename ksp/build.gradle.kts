plugins {
    alias(libs.plugins.kotlin.jvm)
}

group   = "io.github.mcbianconi.quintkonnect"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(project(":annotations"))
    compileOnly(project(":core"))
    compileOnly(libs.ksp.api)
}
