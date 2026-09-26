plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-annotations")
    pom {
        name.set("quint-konnect-annotations")
        description.set("Annotation declarations (@QuintRun, @QuintTest, @QuintAction, @QuintIgnore) for quint-konnect drivers.")
    }
}

dependencies {
    // @QuintIgnore is a @SerialInfo annotation (kotlinx.serialization.SerialInfo), compile-time only:
    // whoever applies it puts it on a @Serializable property, so kotlinx-serialization is already on
    // their own classpath. Keeps this module's "no runtime dependency" property (see AGENTS.md).
    compileOnly(libs.kotlinx.serialization.json)
}
