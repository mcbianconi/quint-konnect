package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

// Exercises DownloadQuintTask.download() directly (a plain method call, no GradleRunner/network
// needed) against a file:// fixture standing in for a GitHub release asset.
class DownloadQuintTaskTest {

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun newTask(projectDir: File): DownloadQuintTask {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        return project.tasks.register("downloadQuintUnderTest", DownloadQuintTask::class.java).get()
    }

    @Test
    fun `downloads and verifies a matching checksum`(@TempDir tempDir: File) {
        val fixtureContent = "pretend this is the quint binary".toByteArray()
        val fixture = File(tempDir, "quint-macos-arm64").apply { writeBytes(fixtureContent) }
        val target = File(tempDir, "out/quint")

        val task = newTask(tempDir)
        task.version.set("0.32.0")
        task.downloadUrl.set(fixture.toURI().toString())
        task.expectedSha256.set(sha256(fixtureContent))
        task.executable.set(target)

        task.download()

        assertTrue(target.exists())
        assertTrue(target.canExecute())
        assertEquals(String(fixtureContent), target.readText())
    }

    @Test
    fun `fails on a checksum mismatch and leaves no output file`(@TempDir tempDir: File) {
        val fixture = File(tempDir, "quint-macos-arm64").apply { writeBytes("real content".toByteArray()) }
        val target = File(tempDir, "out/quint")

        val task = newTask(tempDir)
        task.version.set("0.32.0")
        task.downloadUrl.set(fixture.toURI().toString())
        task.expectedSha256.set("0000000000000000000000000000000000000000000000000000000000000000")
        task.executable.set(target)

        val exception = assertThrows(GradleException::class.java) { task.download() }
        assertTrue(exception.message!!.contains("checksum verification"))
        assertFalse(target.exists())
    }

    @Test
    fun `skips verification when no checksum is known`(@TempDir tempDir: File) {
        val fixture = File(tempDir, "quint-macos-arm64").apply { writeBytes("unverified content".toByteArray()) }
        val target = File(tempDir, "out/quint")

        val task = newTask(tempDir)
        task.version.set("0.0.0-unknown")
        task.downloadUrl.set(fixture.toURI().toString())
        task.executable.set(target)

        task.download()

        assertTrue(target.exists())
        assertEquals("unverified content", target.readText())
    }

    @Test
    fun `is a no-op when the target already exists`(@TempDir tempDir: File) {
        val target = File(tempDir, "out/quint").apply {
            parentFile.mkdirs()
            writeText("already there")
        }

        val task = newTask(tempDir)
        task.version.set("0.32.0")
        task.downloadUrl.set(File(tempDir, "does-not-exist").toURI().toString())
        task.executable.set(target)

        task.download()

        assertEquals("already there", target.readText())
    }

    @Test
    fun `knownQuintSha256 returns the pinned digests for 0-32-0 and null otherwise`() {
        assertEquals(
            "eb038b5e47d9839977053097830eea386f42081780ca7e6a9b6b4ff5f3de131f",
            knownQuintSha256("0.32.0", "macos-arm64"),
        )
        assertEquals(null, knownQuintSha256("0.0.0-does-not-exist", "macos-arm64"))
    }
}
