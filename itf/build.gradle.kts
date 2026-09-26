plugins {
    id("quintkonnect.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // ItfValue's public API exposes Json/JsonElement/KSerializer (QuintJson, toNormalizedJson,
    // ItfValueSerializer, BigIntegerSerializer), so consumers need it on their compile classpath too.
    // https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
