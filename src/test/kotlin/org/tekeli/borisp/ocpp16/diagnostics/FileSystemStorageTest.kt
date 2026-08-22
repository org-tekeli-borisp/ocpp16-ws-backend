package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
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

    @Test
    fun `baseDir returns the configured base directory`() {
        assertEquals(tempDir.toString(), storage.baseDir())
    }

    @Test
    fun `uploadFile allows file exactly at max size`() {
        val smallStorage = FileSystemStorage(tempDir.resolve("small-base").toString(), 10L)
        assertDoesNotThrow {
            smallStorage.uploadFile("CP-001", "exact.log", ByteArray(10).inputStream())
        }
    }

    @Test
    fun `uploadFile stores file prefixed with current epoch second`() {
        val before = Instant.now().epochSecond
        val storedName = storage.uploadFile("CP-001", "diag.log", byteArrayOf(1).inputStream())
        val after = Instant.now().epochSecond
        val prefix = storedName.substringBefore('_').toLong()
        assertTrue(prefix in before..after, "stored name should start with the current epoch second")
    }

    @Test
    fun `listFiles excludes subdirectories`() {
        storage.uploadFile("CP-001", "diag.log", byteArrayOf(1).inputStream())
        val dir = storage.ensureDirectory("CP-001")
        Files.createDirectory(dir.resolve("subdir"))
        assertEquals(1, storage.listFiles("CP-001").size, "only regular files should be listed")
    }

    @Test
    fun `listFiles extracts original name from stored name`() {
        storage.uploadFile("CP-001", "diag.log", byteArrayOf(1).inputStream())
        assertEquals("diag.log", storage.listFiles("CP-001")[0].originalName)
    }

    @Test
    fun `listFiles keeps stored name starting with underscore`() {
        val dir = storage.ensureDirectory("CP-001")
        Files.write(dir.resolve("_diag.log"), byteArrayOf(1))
        assertEquals("_diag.log", storage.listFiles("CP-001")[0].originalName)
    }

    @Test
    fun `listFiles returns a recent uploadedAt timestamp`() {
        storage.uploadFile("CP-001", "diag.log", byteArrayOf(1).inputStream())
        val info = storage.listFiles("CP-001")[0]
        val ageMillis = System.currentTimeMillis() - info.uploadedAt.toEpochMilli()
        assertTrue(ageMillis in 0..60_000, "uploadedAt should reflect the file's recent last-modified time")
    }

    @Test
    fun `listFiles returns files sorted newest first`() {
        val dir = storage.ensureDirectory("CP-001")
        val base = System.currentTimeMillis() - 50_000
        for (i in 0 until 5) {
            val file = dir.resolve("file$i.log")
            Files.write(file, byteArrayOf(1))
            file.toFile().setLastModified(base + i * 10_000)
        }

        val files = storage.listFiles("CP-001")

        assertEquals(5, files.size)
        for (i in 0 until files.size - 1) {
            assertTrue(files[i].uploadedAt >= files[i + 1].uploadedAt, "files must be sorted newest first")
        }
    }

    @Test
    fun `cleanupExpired uses days as retention unit`() {
        storage.uploadFile("CP-001", "onehour.log", byteArrayOf(1).inputStream())
        val path = storage.getFile("CP-001", storage.listFiles("CP-001")[0].storedName)!!
        path.toFile().setLastModified(System.currentTimeMillis() - 60 * 60 * 1000)
        storage.cleanupExpired(30)
        assertEquals(1, storage.listFiles("CP-001").size, "a 1-hour-old file must survive 30-day retention")
    }

    @Test
    fun `cleanupExpired returns 0 when base directory does not exist`() {
        val missingStorage = FileSystemStorage(tempDir.resolve("missing").toString(), 1024)
        assertEquals(0, missingStorage.cleanupExpired(30))
    }

    @Test
    fun `cleanupExpired does not delete subdirectories during file cleanup`() {
        val dir = storage.ensureDirectory("CP-001")
        val subdir = Files.createDirectory(dir.resolve("subdir"))
        storage.uploadFile("CP-001", "old.log", byteArrayOf(1).inputStream())
        val oldFile = storage.getFile("CP-001", storage.listFiles("CP-001")[0].storedName)!!
        val oldTime = System.currentTimeMillis() - 367L * 24 * 60 * 60 * 1000
        oldFile.toFile().setLastModified(oldTime)
        subdir.toFile().setLastModified(oldTime)

        val count = storage.cleanupExpired(30)

        assertEquals(1, count, "only the old regular file should be deleted")
        assertTrue(Files.exists(subdir), "subdirectory should remain after file cleanup")
    }

    @Test
    fun `cleanupExpired does not touch empty directories when nothing expired`() {
        val emptyDir = storage.ensureDirectory("CP-EMPTY")
        storage.cleanupExpired(30)
        assertTrue(Files.exists(emptyDir), "pre-existing empty dir should be kept when no files expired")
    }

    @Test
    fun `cleanupExpired returns the number of deleted files`() {
        storage.uploadFile("CP-001", "old.log", byteArrayOf(1).inputStream())
        val file = storage.getFile("CP-001", storage.listFiles("CP-001")[0].storedName)!!
        file.toFile().setLastModified(System.currentTimeMillis() - 367L * 24 * 60 * 60 * 1000)
        assertEquals(1, storage.cleanupExpired(30))
    }

    @Test
    fun `cleanupExpired deletes emptied CP directories`() {
        val dir = storage.ensureDirectory("CP-001")
        storage.uploadFile("CP-001", "old.log", byteArrayOf(1).inputStream())
        val file = storage.getFile("CP-001", storage.listFiles("CP-001")[0].storedName)!!
        file.toFile().setLastModified(System.currentTimeMillis() - 367L * 24 * 60 * 60 * 1000)

        storage.cleanupExpired(30)

        assertFalse(Files.exists(dir), "emptied CP directory should be deleted")
    }

    @Test
    fun `cleanupExpired keeps non-empty CP directories`() {
        val oldDir = storage.ensureDirectory("CP-OLD")
        val recentDir = storage.ensureDirectory("CP-RECENT")
        storage.uploadFile("CP-OLD", "old.log", byteArrayOf(1).inputStream())
        storage.uploadFile("CP-RECENT", "recent.log", byteArrayOf(1).inputStream())
        val oldFile = storage.getFile("CP-OLD", storage.listFiles("CP-OLD")[0].storedName)!!
        oldFile.toFile().setLastModified(System.currentTimeMillis() - 367L * 24 * 60 * 60 * 1000)

        storage.cleanupExpired(30)

        assertFalse(Files.exists(oldDir), "emptied CP-OLD should be deleted")
        assertTrue(Files.exists(recentDir), "non-empty CP-RECENT should be kept")
    }

    @Test
    fun `getDirectorySize returns 0 for non-existent directory`() {
        assertEquals(0L, storage.getDirectorySize("NO-SUCH-CP"))
    }

    @Test
    fun `getDirectorySize excludes subdirectories`() {
        storage.uploadFile("CP-001", "a.log", ByteArray(100).inputStream())
        val dir = storage.ensureDirectory("CP-001")
        Files.createDirectory(dir.resolve("subdir"))
        assertEquals(100L, storage.getDirectorySize("CP-001"))
    }
}
