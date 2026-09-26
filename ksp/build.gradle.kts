plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-ksp")
    pom {
        name.set("quint-konnect-ksp")
        description.set("KSP2 processor that generates JUnit 5 tests and step dispatchers for quint-konnect drivers.")
    }
}

dependencies {
    compileOnly(project(":annotations"))
    compileOnly(project(":core"))
    compileOnly(libs.ksp.api)
}
