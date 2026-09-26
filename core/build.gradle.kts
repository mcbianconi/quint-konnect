plugins {
    id("quintkonnect.library")
    alias(libs.plugins.kotlin.serialization)
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
