package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QuintPlatformTest {

    @Test
    fun `maps macOS arm64`() {
        val platform = detectQuintPlatform(osName = "Mac OS X", osArch = "aarch64")
        assertEquals(QuintPlatform("macos", "arm64"), platform)
        assertEquals("quint-macos-arm64", platform.assetName)
    }

    @Test
    fun `maps macOS amd64`() {
        assertEquals(QuintPlatform("macos", "amd64"), detectQuintPlatform(osName = "Mac OS X", osArch = "x86_64"))
    }

    @Test
    fun `maps Linux arm64`() {
        assertEquals(QuintPlatform("linux", "arm64"), detectQuintPlatform(osName = "Linux", osArch = "aarch64"))
    }

    @Test
    fun `maps Linux amd64`() {
        assertEquals(QuintPlatform("linux", "amd64"), detectQuintPlatform(osName = "Linux", osArch = "amd64"))
    }

    @Test
    fun `rejects Windows`() {
        val exception = assertThrows(GradleException::class.java) {
            detectQuintPlatform(osName = "Windows 11", osArch = "amd64")
        }
        assertEquals(
            "quintKonnect.downloadQuint does not support OS \"Windows 11\": quint-konnect doesn't target " +
                "Windows (docs/decisions/no-windows-support.md). Install quint manually and leave " +
                "downloadQuint disabled.",
            exception.message,
        )
    }

    @Test
    fun `rejects an unknown CPU architecture`() {
        val exception = assertThrows(GradleException::class.java) {
            detectQuintPlatform(osName = "Linux", osArch = "riscv64")
        }
        assertEquals("quintKonnect.downloadQuint does not support CPU architecture \"riscv64\".", exception.message)
    }
}
