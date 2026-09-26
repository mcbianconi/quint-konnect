plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
    alias(libs.plugins.kotlin.serialization)
}

mavenPublishing {
    coordinates(artifactId = "itf-kotlin")
    pom {
        name.set("itf-kotlin")
        description.set("ITF trace parsing and value normalization for quint-konnect.")
    }
}

dependencies {
    // The public API exposes kotlinx.serialization types (ItfValue.decode(DeserializationStrategy),
    // ItfValueSerializer, BigIntegerSerializer), so consumers need it on their compile classpath too.
    // https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()

    val typesMd = layout.projectDirectory.file("../skills/quint-konnect/references/types.md")
    systemProperty("quintKonnect.typesMdPath", typesMd.asFile.absolutePath)
    // https://docs.gradle.org/current/userguide/incremental_build.html#sec:task_input_output_runtime_api
    inputs.file(typesMd).withPathSensitivity(PathSensitivity.RELATIVE)
}
