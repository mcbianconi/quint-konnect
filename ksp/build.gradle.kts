plugins {
    id("quintkonnect.library")
}

dependencies {
    compileOnly(project(":annotations"))
    compileOnly(project(":core"))
    compileOnly(libs.ksp.api)
}
