plugins {
    id("quintkonnect.library")
    id("quintkonnect.publish")
}

mavenPublishing {
    coordinates(artifactId = "quint-konnect-annotations")
    pom {
        name.set("quint-konnect-annotations")
        description.set("Annotation declarations (@QuintRun, @QuintTest, @QuintAction) for quint-konnect drivers.")
    }
}
