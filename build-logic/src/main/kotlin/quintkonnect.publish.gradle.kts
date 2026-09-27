// Maven Central publishing via the vanniktech gradle-maven-publish-plugin (Central Portal
// support, signing, sources/javadoc jars). Base plugin docs (manual, explicit configuration):
// https://vanniktech.github.io/gradle-maven-publish-plugin/base/
// POM/coordinates DSL: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
// Applied by annotations/itf/core/ksp (qk-j02b); each of those modules sets its own
// artifactId and POM name/description. :example and :integration-tests are not published.
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.vanniktech.maven.publish.base")
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    // Only sign when credentials are present, so `publishToMavenLocal` works without keys
    // locally. In CI (.github/workflows/release.yml) the SIGNING_KEY secret is mapped to
    // ORG_GRADLE_PROJECT_signingInMemoryKey, which Gradle exposes as this project property.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    // No Dokka set up in this project, so an empty javadoc jar (Central requires one to be
    // present, even if empty) plus the real sources jar.
    configureBasedOnAppliedPlugins(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = SourcesJar.Sources(),
    )

    pom {
        url.set("https://github.com/mcbianconi/quint-konnect")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("mcbianconi")
                name.set("Murillo Cesar Bianconi")
            }
        }
        scm {
            url.set("https://github.com/mcbianconi/quint-konnect")
            connection.set("scm:git:git://github.com/mcbianconi/quint-konnect.git")
            developerConnection.set("scm:git:ssh://git@github.com/mcbianconi/quint-konnect.git")
        }
    }
}
