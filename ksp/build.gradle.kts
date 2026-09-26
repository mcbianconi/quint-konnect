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

    // The processor's own compileOnly deps need to be on the test classpath too, since
    // kotlin-compile-testing (inheritClassPath = true) compiles and loads driver fixtures
    // against this module's test classpath.
    testImplementation(project(":annotations"))
    testImplementation(project(":core"))
    testImplementation(libs.ksp.api)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
