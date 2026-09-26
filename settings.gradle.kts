// https://docs.gradle.org/current/userguide/multi_project_builds.html
// https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html#sec:sharing_logic_via_composite_build
pluginManagement {
    includeBuild("build-logic")
}

// https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_dependency_resolution_management
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "quint-konnect"

include(":annotations", ":itf", ":core", ":ksp", ":gradle-plugin", ":example")
