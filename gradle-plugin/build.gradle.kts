// Gradle plugin development: https://docs.gradle.org/current/userguide/java_gradle_plugin.html
// Functional tests via TestKit: https://docs.gradle.org/current/userguide/test_kit.html
// Publishing a java-gradle-plugin project (main + marker artifact) via the vanniktech plugin:
// https://vanniktech.github.io/gradle-maven-publish-plugin/what/#gradle-plugin
// `java-gradle-plugin` must apply before `quintkonnect.publish` below: that convention plugin's
// `configureBasedOnAppliedPlugins` picks its Platform (here GradlePlugin, publishing the plugin
// marker artifact alongside the main jar) from whichever publishing-relevant plugins are already
// applied at that point, and calling `configure(...)` a second time to override it later fails
// ("value for this property is final").
plugins {
    id("quintkonnect.library")
    `java-gradle-plugin`
    id("quintkonnect.publish")
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-gradle-plugin")
    pom {
        name.set("quint-konnect-gradle-plugin")
        description.set(
            "Gradle plugin for quint-konnect: applies KSP and dependencies, resolves spec paths, " +
                "and checks the quint CLI.",
        )
    }
}

gradlePlugin {
    plugins {
        register("quintKonnect") {
            id = "io.github.mcbianconi.quint-konnect"
            implementationClass = "io.github.mcbianconi.quintkonnect.gradle.QuintKonnectPlugin"
            displayName = "quint-konnect"
            description = "Applies KSP and quint-konnect dependencies, resolves spec paths, and checks the quint CLI."
        }
    }
}

// Bakes this module's version into a generated constant read by QuintKonnectPlugin, since the
// jar's MANIFEST.MF Implementation-Version isn't available when TestKit loads the plugin from
// class directories (functionalTest below) rather than a jar.
val generatePluginVersion = tasks.register("generatePluginVersion") {
    val outputDir = layout.buildDirectory.dir("generated/pluginVersion/kotlin")
    val pluginVersion = project.version.toString()
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/mcbianconi/quintkonnect/gradle/PluginVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package io.github.mcbianconi.quintkonnect.gradle\n\n" +
                "internal const val PLUGIN_VERSION: String = \"$pluginVersion\"\n",
        )
    }
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generatePluginVersion)
    }
}

// https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-automatic-classpath-injection
val functionalTest = sourceSets.create("functionalTest")

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    // The fixture project applies kotlin.jvm itself (with a version, resolved normally); keep it
    // in step with this project's own `kotlin` catalog version instead of hardcoding it in the test.
    systemProperty("quintkonnect.fixtureKotlinVersion", libs.versions.kotlin.get())
}

tasks.named("check") {
    dependsOn(functionalTestTask)
}

gradlePlugin {
    testSourceSets(functionalTest)
}

dependencies {
    // The KSP Gradle plugin extends Kotlin compile task types, so it and the Kotlin Gradle plugin
    // must resolve from the same classpath/classloader as each other here too (same reasoning as
    // build-logic/build.gradle.kts), including under TestKit's plugin-under-test classloader.
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)

    // `gradlePlugin { testSourceSets(functionalTest) }` above means the automatic gradleTestKit()
    // injection (https://docs.gradle.org/current/userguide/java_gradle_plugin.html#header)
    // targets functionalTest, not test: ProjectBuilder (used by the plain unit tests here) needs
    // it declared explicitly on this configuration instead.
    testImplementation(gradleTestKit())
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // The functional tests reference QUINT_INSTALL_COMMAND/DEFAULT_QUINT_VERSION from main.
    "functionalTestImplementation"(sourceSets.main.get().output)
    "functionalTestImplementation"(libs.junit5.api)
    "functionalTestRuntimeOnly"(libs.junit5.engine)
    "functionalTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
