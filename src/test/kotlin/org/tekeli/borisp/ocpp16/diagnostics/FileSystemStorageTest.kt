package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant

class FileSystemStorageTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileSystemStorage

    @BeforeEach
    fun setup() {
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
    }

    @Test
    fun `ensureDirectory creates CP subdirectory`() {
        val dir = storage.ensureDirectory("CP-001")
        assertTrue(dir.toFile().exists())
        assertEquals("CP-001", dir.fileName.toString())
    }

    @Test
    fun `ensureDirectory is idempotent`() {
        val dir1 = storage.ensureDirectory("CP-001")
        val dir2 = storage.ensureDirectory("CP-001")
        assertEquals(dir1, dir2)
    }

    @Test
    fun `uploadFile saves file and returns stored name`() {
        val storedName = storage.uploadFile("CP-001", "diag.log", byteArrayOf(1, 2, 3).inputStream())
        assertNotNull(storedName)
        assertTrue(storedName.endsWith(".log"))
        val dir = storage.ensureDirectory("CP-001")
        assertTrue(dir.toFile().listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `uploadFile rejects file exceeding max size`() {
        val largeContent = ByteArray(11 * 1024 * 1024)
        assertThrows(IllegalArgumentException::class.java) {
            storage.uploadFile("CP-001", "huge.log", largeContent.inputStream())
        }
    }

    @Test
    fun `getFile returns path for existing file`() {
        val storedName = storage.uploadFile("CP-001", "diag.log", byteArrayOf(1, 2, 3).inputStream())
        val path = storage.getFile("CP-001", storedName)
        assertNotNull(path)
        assertTrue(path!!.toFile().exists())
    }

    @Test
    fun `getFile returns null for non-existent file`() {
        val path = storage.getFile("CP-001", "nonexistent.log")
        assertNull(path)
    }

    @Test
    fun `listFiles returns empty list for new CP`() {
        val files = storage.listFiles("CP-001")
        assertTrue(files.isEmpty())
    }

    @Test
    fun `listFiles returns uploaded files`() {
        storage.uploadFile("CP-001", "diag1.log", byteArrayOf(1).inputStream())
        storage.uploadFile("CP-001", "diag2.log", byteArrayOf(2).inputStream())
        val files = storage.listFiles("CP-001")
        assertEquals(2, files.size)
    }

    @Test
    fun `listFiles includes file metadata`() {
        storage.uploadFile("CP-001", "diag.log", "content".toByteArray(Charsets.UTF_8).inputStream())
        val files = storage.listFiles("CP-001")
        assertEquals(1, files.size)
        val info = files[0]
        assertTrue(info.originalName.endsWith(".log"))
        assertEquals(7L, info.sizeBytes)
        assertNotNull(info.uploadedAt)
    }

    @Test
    fun `deleteFile removes file`() {
        val storedName = storage.uploadFile("CP-001", "diag.log", byteArrayOf(1).inputStream())
        val deleted = storage.deleteFile("CP-001", storedName)
        assertTrue(deleted)
        assertNull(storage.getFile("CP-001", storedName))
    }

    @Test
    fun `deleteFile returns false for non-existent`() {
        val deleted = storage.deleteFile("CP-001", "nonexistent.log")
        assertFalse(deleted)
    }

    @Test
    fun `cleanupExpired removes files older than retention days`() {
        storage.uploadFile("CP-001", "old.log", byteArrayOf(1).inputStream())
        val file = storage.getFile("CP-001", storage.listFiles("CP-001")[0].storedName)!!
        file.toFile().setLastModified(System.currentTimeMillis() - (367L * 24 * 60 * 60 * 1000))
        storage.cleanupExpired(30)
        assertTrue(storage.listFiles("CP-001").isEmpty())
    }

    @Test
    fun `cleanupExpired keeps recent files`() {
        storage.uploadFile("CP-001", "recent.log", byteArrayOf(1).inputStream())
        storage.cleanupExpired(30)
        assertEquals(1, storage.listFiles("CP-001").size)
    }

    @Test
    fun `getDirectorySize returns correct total size`() {
        storage.uploadFile("CP-001", "a.log", ByteArray(100).inputStream())
        storage.uploadFile("CP-001", "b.log", ByteArray(200).inputStream())
        val size = storage.getDirectorySize("CP-001")
        assertEquals(300L, size)
    }
}
