plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
    alias(libs.plugins.kotlin.serialization)
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-core")
    pom {
        name.set("quint-konnect-core")
        description.set("Runtime for quint-konnect: quint CLI invocation, trace generation, and replay.")
    }
}

dependencies {
    api(project(":annotations"))
    api(project(":itf"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
