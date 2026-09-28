package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException

// quint-konnect doesn't target Windows (docs/decisions/no-windows-support.md), so downloadQuint
// only maps the OS/CPU combinations quint publishes standalone binaries for on macOS and Linux:
// https://github.com/quint-co/quint/releases (asset names verified against v0.32.0).
internal data class QuintPlatform(val os: String, val arch: String) {
    val tag: String get() = "$os-$arch"
    val assetName: String get() = "quint-$tag"
}

internal fun detectQuintPlatform(
    osName: String = System.getProperty("os.name") ?: "",
    osArch: String = System.getProperty("os.arch") ?: "",
): QuintPlatform {
    val os = when {
        osName.contains("mac", ignoreCase = true) || osName.contains("darwin", ignoreCase = true) -> "macos"

        osName.contains("linux", ignoreCase = true) -> "linux"

        else -> throw GradleException(
            "quintKonnect.downloadQuint does not support OS \"$osName\": quint-konnect doesn't target Windows " +
                "(docs/decisions/no-windows-support.md). Install quint manually and leave downloadQuint disabled.",
        )
    }
    val arch = when {
        osArch.contains("aarch64", ignoreCase = true) || osArch.contains("arm64", ignoreCase = true) -> "arm64"
        osArch.contains("amd64", ignoreCase = true) || osArch.contains("x86_64", ignoreCase = true) -> "amd64"
        else -> throw GradleException("quintKonnect.downloadQuint does not support CPU architecture \"$osArch\".")
    }
    return QuintPlatform(os, arch)
}
