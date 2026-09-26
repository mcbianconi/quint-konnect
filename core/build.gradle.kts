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

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
