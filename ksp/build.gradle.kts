plugins {
    id("quintkonnect.kotlin-jvm")
}

dependencies {
    compileOnly(project(":annotations"))
    compileOnly(project(":core"))
    compileOnly(libs.ksp.api)
}
