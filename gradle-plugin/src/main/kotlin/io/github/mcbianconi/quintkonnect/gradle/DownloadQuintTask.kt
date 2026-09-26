package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

// sha256 digests GitHub's releases API reports for each v0.32.0 asset (the pinned default
// version), fetched with `gh api repos/quint-co/quint/releases/tags/v0.32.0 --jq
// '.assets[]|"\(.name) \(.digest)"'`. quint doesn't publish its own checksums file; a version not
// listed here downloads without verification (a warning is logged).
private val KNOWN_SHA256: Map<String, String> = mapOf(
    "0.32.0/linux-amd64" to "939b64095b706017f2f202c6f99c860c40be7c31bddc2b98557316e50f42cd7f",
    "0.32.0/linux-arm64" to "5b23e6f7e6f6b9c870c5ea7d38675e8fc709f4578bcf4a236918414157267a35",
    "0.32.0/macos-amd64" to "83e731bfe634b2041b1444d3a283055e70608c9894650a6da08dd1db22428fef",
    "0.32.0/macos-arm64" to "eb038b5e47d9839977053097830eea386f42081780ca7e6a9b6b4ff5f3de131f",
)

internal fun knownQuintSha256(version: String, platformTag: String): String? = KNOWN_SHA256["$version/$platformTag"]

// quint's GitHub releases publish standalone per-OS/arch binaries compiled with `deno compile`
// (bundles the Deno runtime, no separate Node/quint install needed): see
// https://github.com/quint-co/quint/blob/main/.github/upload-binaries.sh. informalsystems/quint
// redirects to quint-co/quint; asset names verified against the v0.32.0 release.
@DisableCachingByDefault(
    because = "Downloads a large (~120MB) external binary into the Gradle user home cache; " +
        "caching it in the build cache too would just duplicate that cache.",
)
public abstract class DownloadQuintTask : DefaultTask() {

    @get:Input
    public abstract val version: Property<String>

    // Not exposed through the quintKonnect extension: lets a test point this at a local file://
    // fixture instead of GitHub.
    @get:Input
    public abstract val downloadUrl: Property<String>

    @get:Input
    @get:Optional
    public abstract val expectedSha256: Property<String>

    @get:OutputFile
    public abstract val executable: RegularFileProperty

    init {
        group = "verification"
        description = "Downloads the pinned quint CLI into the Gradle user home cache."
    }

    @TaskAction
    public fun download() {
        val target = executable.get().asFile
        if (target.exists()) {
            logger.info("quint {} already downloaded at {}", version.get(), target)
            return
        }

        target.parentFile.mkdirs()
        val tmpFile = File.createTempFile("quint-", ".download", target.parentFile)
        try {
            URI(downloadUrl.get()).toURL().openStream().use { input ->
                Files.copy(input, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            val expected = expectedSha256.orNull
            if (expected == null) {
                logger.warn(
                    "No known checksum for quint {}; the downloaded binary was not verified.",
                    version.get(),
                )
            } else {
                val actual = sha256(tmpFile)
                if (!actual.equals(expected, ignoreCase = true)) {
                    throw GradleException(
                        "Downloaded quint ${version.get()} failed checksum verification: " +
                            "expected $expected, got $actual",
                    )
                }
            }

            tmpFile.setExecutable(true)
            Files.move(
                tmpFile.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            tmpFile.delete()
        }
    }
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } >= 0) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
