package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

/**
 * True when a test source annotates a driver with `QuintRun` or `QuintTest`, either as
 * `@QuintRun` or as a fully qualified `@…QuintRun`.
 *
 * Read through a [ValueSource] so configuration cache can fingerprint the files. A project with
 * no drivers must not gain a `checkQuint` dependency (qk-blt0).
 */
internal abstract class QuintDriverSources : ValueSource<Boolean, QuintDriverSources.Params> {
    internal interface Params : ValueSourceParameters {
        val roots: ConfigurableFileCollection
    }

    override fun obtain(): Boolean =
        parameters.roots.any { root ->
            root.isDirectory && root.walkTopDown().any { file ->
                file.isFile && file.extension in setOf("kt", "java") && mentionsQuintDriver(file)
            }
        }

    private fun mentionsQuintDriver(file: File): Boolean =
        file.useLines { lines ->
            lines.any { line -> driverAnnotation.containsMatchIn(line.substringBefore("//")) }
        }

    private companion object {
        // `@QuintRun` and `@pkg.QuintRun` both count. A longer identifier such as `QuintRunConfig` does not.
        val driverAnnotation: Regex = Regex("""(?:@|\.)Quint(?:Run|Test)\b""")
    }
}
