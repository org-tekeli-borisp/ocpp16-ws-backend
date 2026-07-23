package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.logging.Log
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeUnit

data class DiagnosticsFileInfo(
    val storedName: String,
    val originalName: String,
    val sizeBytes: Long,
    val uploadedAt: Instant
)

class FileSystemStorage(
    private val baseDir: String,
    private val maxFileSizeBytes: Long
) {

    fun baseDir(): String = baseDir

    fun ensureDirectory(chargePointId: String): Path {
        val dir = Path.of(baseDir, chargePointId)
        Files.createDirectories(dir)
        return dir
    }

    fun uploadFile(chargePointId: String, originalFileName: String, inputStream: java.io.InputStream): String {
        val dir = ensureDirectory(chargePointId)
        val bytes = ByteArrayOutputStream().use { inputStream.copyTo(it); it.toByteArray() }
        if (bytes.size > maxFileSizeBytes) {
            throw IllegalArgumentException("File size ${bytes.size} exceeds maximum $maxFileSizeBytes bytes")
        }
        val timestamp = Instant.now().epochSecond
        val storedName = "${timestamp}_${originalFileName}"
        val targetPath = dir.resolve(storedName)
        Files.write(targetPath, bytes)
        return storedName
    }

    fun getFile(chargePointId: String, storedFileName: String): Path? {
        val dir = Path.of(baseDir, chargePointId)
        val file = dir.resolve(storedFileName)
        return if (Files.exists(file)) file else null
    }

    fun listFiles(chargePointId: String): List<DiagnosticsFileInfo> {
        val dir = Path.of(baseDir, chargePointId)
        if (!Files.exists(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .map { file ->
                    val storedName = file.fileName.toString()
                    DiagnosticsFileInfo(
                        storedName = storedName,
                        originalName = extractOriginalName(storedName),
                        sizeBytes = Files.size(file),
                        uploadedAt = Instant.ofEpochMilli(file.toFile().lastModified())
                    )
                }
                .toList().sortedByDescending { it.uploadedAt }
        }
    }

    fun deleteFile(chargePointId: String, storedFileName: String): Boolean {
        val file = getFile(chargePointId, storedFileName)
            ?: return false
        return Files.deleteIfExists(file)
    }

    fun cleanupExpired(retentionDays: Int): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        var count = 0
        val base = Path.of(baseDir)
        if (!Files.exists(base)) return 0
        Files.walk(base).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.toFile().lastModified() < cutoff }
                .forEach { file ->
                    try {
                        Files.deleteIfExists(file)
                        count++
                    } catch (e: Exception) {
                        Log.warn("Failed to delete expired file ${file}: ${e.message}")
                    }
                }
        }
        if (count > 0) {
            Files.list(base).use { dirStream ->
                dirStream.filter { dir ->
                    Files.isDirectory(dir) && dir.toFile().listFiles()?.isEmpty() == true
                }.forEach { dir ->
                    try {
                        Files.deleteIfExists(dir)
                    } catch (e: Exception) {
                        Log.warn("Failed to delete empty directory ${dir}: ${e.message}")
                    }
                }
            }
        }
        return count
    }

    fun getDirectorySize(chargePointId: String): Long {
        val dir = Path.of(baseDir, chargePointId)
        if (!Files.exists(dir)) return 0L
        return Files.list(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .mapToLong { Files.size(it) }
                .sum()
        }
    }

    private fun extractOriginalName(storedName: String): String {
        val idx = storedName.indexOf('_')
        return if (idx > 0) storedName.substring(idx + 1) else storedName
    }
}
