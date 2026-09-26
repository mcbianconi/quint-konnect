// https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
// (plugin marker coordinates, so this shares a classloader with alias(libs.plugins.kotlin.jvm)
// resolutions elsewhere in the build instead of loading a second copy of the Kotlin Gradle plugin)
plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))
    // ksp creates Task subtypes that extend Kotlin compile task types, so it must resolve
    // the Kotlin Gradle plugin from this same classpath/classloader as kotlin.jvm above, or
    // applying it fails with "Could not generate a decorated class for type KspGradleSubplugin".
    implementation(plugin(libs.plugins.ksp))
    // the publish plugin reacts to the org.jetbrains.kotlin.jvm plugin (Kotlin source sets,
    // sources jar), so it must resolve the Kotlin Gradle plugin from this same
    // classpath/classloader as kotlin.jvm above too.
    implementation(plugin(libs.plugins.vanniktech.publish))
}

fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
