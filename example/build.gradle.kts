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
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
