plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-ksp")
    pom {
        name.set("quint-konnect-ksp")
        description.set("KSP2 processor that generates JUnit Jupiter tests and step dispatchers for quint-konnect drivers.")
    }
}

dependencies {
    compileOnly(project(":annotations"))
    compileOnly(project(":core"))
    compileOnly(libs.ksp.api)
    // implementation, not compileOnly: KSP runs the processor in its own classloader, so
    // kotlinpoet-ksp must be on the processor's runtime classpath, not just its compile classpath.
    implementation(libs.kotlinpoet.ksp)

    // The processor's own compileOnly deps need to be on the test classpath too, since
    // kotlin-compile-testing (inheritClassPath = true) compiles and loads driver fixtures
    // against this module's test classpath.
    testImplementation(project(":annotations"))
    testImplementation(project(":core"))
    testImplementation(libs.ksp.api)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    // Pins the version driver fixtures compile a suspend @QuintAction's generated `runBlocking`
    // call against explicitly (qk-33ky), rather than relying on the version the Kotlin compiler
    // tooling itself transitively pulls in via kctfork.
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
