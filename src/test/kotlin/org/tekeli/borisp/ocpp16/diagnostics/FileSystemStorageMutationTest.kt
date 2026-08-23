package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class FileSystemStorageMutationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileSystemStorage

    @BeforeEach
    fun setup() {
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
    }

    private fun oldTimeMillis(): Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(367)

    @Test
    fun `baseDir returns the exact configured path`() {
        val base = tempDir.resolve("nested").resolve("storage")
        Files.createDirectories(base)
        val storage = FileSystemStorage(base.toString(), 1024)
        assertEquals(base.toString(), storage.baseDir())
    }

    @Test
    fun `deleteFile returns the actual delete result`() {
        val storedName = storage.uploadFile("CP-001", "x.log", byteArrayOf(1).inputStream())

        assertTrue(storage.deleteFile("CP-001", storedName), "deleting an existing file must return the true result of the deletion")
        assertNull(storage.getFile("CP-001", storedName))
    }

    @Test
    fun `cleanupExpired deletes a file exactly at the cutoff boundary`() {
        val base = tempDir.resolve("boundary")
        val cp = Files.createDirectories(base.resolve("CP-1"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
        val retentionMillis = TimeUnit.DAYS.toMillis(367L)

        val offsets = longArrayOf(-retentionMillis - 5_000, -retentionMillis + 5_000)
        for (i in offsets.indices) {
            if (!Files.exists(cp)) {
                Files.createDirectories(cp)
            }
            if (!Files.exists(file)) {
                Files.write(file, byteArrayOf(1))
            }
            file.toFile().setLastModified(System.currentTimeMillis() + offsets[i])
            val count = storage.cleanupExpired(367)
            if (offsets[i] < -retentionMillis) {
                assertEquals(1, count, "file just before the cutoff must be deleted")
                assertFalse(Files.exists(file), "expired file must be gone")
            } else {
                assertEquals(0, count, "file just after the cutoff must survive")
                assertTrue(Files.exists(file), "recent file must remain")
            }
        }
    }

    @Test
    fun `cleanupExpired logs the exception message when file deletion fails`() {
        val captured = mutableListOf<String>()
        val handler = object : java.util.logging.Handler() {
            override fun publish(record: java.util.logging.LogRecord) {
                record.message?.let { captured.add(it) }
            }

            override fun flush() {}
            override fun close() {}
        }
        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.addHandler(handler)
        try {
            val base = tempDir.resolve("logfail")
            val cp = Files.createDirectories(base.resolve("CP-1"))
            val storage = FileSystemStorage(base.toString(), 1024)
            val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
            file.toFile().setLastModified(oldTimeMillis())

            if (System.getProperty("os.name").startsWith("Linux")) {
                cp.toFile().setReadOnly()
                try {
                    assertEquals(0, storage.cleanupExpired(30), "undeletable file must not be counted")
                    assertTrue(
                        captured.any { it.contains("old.log") && it.contains("Failed to delete expired file") },
                        "warning log must contain the failed file path and reason, got: $captured"
                    )
                } finally {
                    cp.toFile().setWritable(true)
                }
            } else {
                assertDoesNotThrow { storage.cleanupExpired(30) }
            }
        } finally {
            rootLogger.removeHandler(handler)
        }
    }

    @Test
    fun `cleanupExpired logs the exception message when directory deletion fails`() {
        val captured = mutableListOf<String>()
        val handler = object : java.util.logging.Handler() {
            override fun publish(record: java.util.logging.LogRecord) {
                record.message?.let { captured.add(it) }
            }

            override fun flush() {}
            override fun close() {}
        }
        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.addHandler(handler)
        try {
            val base = tempDir.resolve("dirlogfail")
            val cp = Files.createDirectories(base.resolve("CP-1"))
            val storage = FileSystemStorage(base.toString(), 1024)
            val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
            file.toFile().setLastModified(oldTimeMillis())

            if (System.getProperty("os.name").startsWith("Linux")) {
                base.toFile().setReadOnly()
                try {
                    storage.cleanupExpired(30)
                    assertTrue(
                        captured.any { it.contains("CP-1") && it.contains("Failed to delete empty directory") },
                        "warning log must contain the failed directory and reason, got: $captured"
                    )
                } finally {
                    base.toFile().setWritable(true)
                }
            } else {
                assertDoesNotThrow { storage.cleanupExpired(30) }
            }
        } finally {
            rootLogger.removeHandler(handler)
        }
    }

    @Test
    fun `cleanupExpired keeps directories that are not empty`() {
        val base = tempDir.resolve("keepdir")
        val cp1 = Files.createDirectories(base.resolve("CP-1"))
        val cp2 = Files.createDirectories(base.resolve("CP-2"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val oldFile = Files.write(cp1.resolve("old.log"), byteArrayOf(1))
        oldFile.toFile().setLastModified(oldTimeMillis())
        Files.write(cp2.resolve("recent.log"), byteArrayOf(1))

        storage.cleanupExpired(30)

        assertFalse(Files.exists(cp1), "emptied CP-1 should be deleted")
        assertTrue(Files.isDirectory(cp2), "non-empty CP-2 must survive")
    }

    @Test
    fun `cleanupExpired keeps CP directories containing non-empty subdirectories`() {
        val base = tempDir.resolve("subdirkeep")
        val cp = Files.createDirectories(base.resolve("CP-1"))
        val subdir = Files.createDirectory(cp.resolve("nested"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val oldFile = Files.write(cp.resolve("old.log"), byteArrayOf(1))
        oldFile.toFile().setLastModified(oldTimeMillis())
        Files.write(subdir.resolve("inner.log"), byteArrayOf(1))

        val count = storage.cleanupExpired(30)

        assertEquals(1, count, "only the expired regular file should be deleted")
        assertTrue(Files.isDirectory(cp), "CP-1 must survive while its subdirectory still holds a file")
        assertTrue(Files.exists(subdir.resolve("inner.log")), "file inside nested subdirectory must survive")
    }

    @Test
    fun `listFiles returns an empty list when the CP directory exists but is empty`() {
        val base = tempDir.resolve("emptycp")
        Files.createDirectories(base.resolve("CP-EMPTY"))
        val storage = FileSystemStorage(base.toString(), 1024)

        assertEquals(emptyList<DiagnosticsFileInfo>(), storage.listFiles("CP-EMPTY"))
    }

    @Test
    fun `deleteFile returns false when the file does not exist`() {
        assertFalse(storage.deleteFile("CP-NOPE", "ghost.log"), "missing file must yield a false delete result")
    }

    @Test
    fun `cleanupExpired deletes only files at or before the cutoff`() {
        val base = tempDir.resolve("mixed")
        val cp = Files.createDirectories(base.resolve("CP-MIX"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val retentionMillis = TimeUnit.DAYS.toMillis(367L)
        val oldFile = Files.write(cp.resolve("old.log"), byteArrayOf(1))
        val newFile = Files.write(cp.resolve("new.log"), byteArrayOf(1))
        oldFile.toFile().setLastModified(System.currentTimeMillis() - retentionMillis - 10_000)
        newFile.toFile().setLastModified(System.currentTimeMillis() - 60_000)

        val count = storage.cleanupExpired(367)

        assertEquals(1, count, "only the expired file must be deleted")
        assertFalse(Files.exists(oldFile), "expired file must be gone")
        assertTrue(Files.exists(newFile), "recent file must remain")
    }

    @Test
    fun `cleanupExpired does not delete a directory that still contains files`() {
        val base = tempDir.resolve("dirkeep")
        val cp = Files.createDirectories(base.resolve("CP-KEEP"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val oldFile = Files.write(cp.resolve("old.log"), byteArrayOf(1))
        oldFile.toFile().setLastModified(oldTimeMillis())
        Files.write(cp.resolve("keep.log"), byteArrayOf(1))

        storage.cleanupExpired(30)

        assertTrue(Files.isDirectory(cp), "directory with remaining files must not be deleted")
        assertTrue(Files.exists(cp.resolve("keep.log")))
    }

    @Test
    fun `cleanupExpired does not delete a non-empty nested subdirectory`() {
        val base = tempDir.resolve("nestedkeep")
        val cp = Files.createDirectories(base.resolve("CP-N"))
        val nested = Files.createDirectory(cp.resolve("deep"))
        val storage = FileSystemStorage(base.toString(), 1024)
        val oldFile = Files.write(cp.resolve("old.log"), byteArrayOf(1))
        oldFile.toFile().setLastModified(oldTimeMillis())
        Files.write(nested.resolve("inner.log"), byteArrayOf(1))

        storage.cleanupExpired(30)

        assertTrue(Files.exists(nested.resolve("inner.log")), "file in non-empty nested dir must survive")
    }
}
