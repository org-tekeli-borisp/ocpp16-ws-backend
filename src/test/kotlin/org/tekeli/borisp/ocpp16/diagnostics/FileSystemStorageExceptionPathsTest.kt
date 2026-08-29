package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.Answers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mockStatic
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class FileSystemStorageExceptionPathsTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileSystemStorage

    @BeforeEach
    fun setup() {
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
    }

    private fun throwingStream(): Stream<Path> = Stream.generate<Path> { throw IOException("stream failure") }

    @Test
    fun `uploadFile propagates exception when input stream read fails`() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("read failure")
        }

        assertThrows<IOException> { storage.uploadFile("CP-001", "x.log", broken) }
    }

    @Test
    fun `listFiles propagates exception when stream iteration fails`() {
        Files.createDirectories(tempDir.resolve("CP-001"))

        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Stream<Path>> { Files.list(any<Path>()) }.thenReturn(throwingStream())
            assertThrows<IOException> { storage.listFiles("CP-001") }
        }
    }

    @Test
    fun `cleanupExpired propagates exception when walk stream iteration fails`() {
        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Stream<Path>> { Files.walk(any<Path>()) }.thenReturn(throwingStream())
            assertThrows<IOException> { storage.cleanupExpired(30) }
        }
    }

    @Test
    fun `cleanupExpired propagates exception when directory stream iteration fails`() {
        val cpDir = Files.createDirectories(tempDir.resolve("CP-001"))
        val expired = Files.write(cpDir.resolve("old.log"), "old".toByteArray())
        Files.setLastModifiedTime(expired, FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))

        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Stream<Path>> { Files.list(any<Path>()) }.thenReturn(throwingStream())
            assertThrows<IOException> { storage.cleanupExpired(1) }
        }
    }

    @Test
    fun `getDirectorySize propagates exception when stream iteration fails`() {
        Files.createDirectories(tempDir.resolve("CP-001"))

        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Stream<Path>> { Files.list(any<Path>()) }.thenReturn(throwingStream())
            assertThrows<IOException> { storage.getDirectorySize("CP-001") }
        }
    }
}
