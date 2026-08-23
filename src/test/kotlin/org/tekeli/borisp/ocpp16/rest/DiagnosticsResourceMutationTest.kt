package org.tekeli.borisp.ocpp16.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tekeli.borisp.ocpp16.diagnostics.FileSystemStorage
import java.io.InputStream
import java.nio.file.Path

class DiagnosticsResourceMutationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileSystemStorage
    private lateinit var resource: DiagnosticsResource

    @BeforeEach
    fun setup() {
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
        resource = DiagnosticsResource()
        resource.storage = storage
    }

    private fun seed(chargePointId: String, originalName: String, content: ByteArray): String =
        storage.uploadFile(chargePointId, originalName, content.inputStream())

    @Test
    fun `list throws UninitializedPropertyAccessException when storage is not injected`() {
        val bare = DiagnosticsResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.list("CP-001")
        }
    }

    @Test
    fun `list maps each stored file to its metadata fields`() {
        val content = byteArrayOf(1, 2, 3, 4, 5)
        val storedName = seed("CP-001", "report.log", content)

        val result = resource.list("CP-001")

        assertEquals(1, result.size)
        val entry = result[0]
        assertEquals(storedName, entry["storedName"])
        assertEquals("report.log", entry["originalName"])
        assertEquals(content.size.toLong(), entry["sizeBytes"])
        assertTrue(entry["uploadedAt"] is String, "uploadedAt must be rendered as a String")
    }

    @Test
    fun `list returns empty list for a charge point without files`() {
        assertTrue(resource.list("CP-EMPTY").isEmpty())
    }

    @Test
    fun `download returns 200 with file bytes and headers for an existing file`() {
        val content = byteArrayOf(9, 8, 7, 6)
        val storedName = seed("CP-001", "diag.bin", content)

        val response = resource.download("CP-001", storedName)

        assertEquals(200, response.status)
        val body = response.entity as InputStream
        assertArrayEquals(content, body.readBytes())
        body.close()
        assertEquals("attachment; filename=\"$storedName\"", response.getHeaderString("Content-Disposition"))
        assertEquals(content.size.toString(), response.getHeaderString("Content-Length"))
    }

    @Test
    fun `download returns 404 with error body for a missing file`() {
        val response = resource.download("CP-001", "missing.log")

        assertEquals(404, response.status)
        @Suppress("UNCHECKED_CAST")
        val entity = response.entity as Map<String, String>
        assertEquals("File not found: missing.log", entity["error"])
    }

    @Test
    fun `delete returns 204 for an existing file`() {
        val storedName = seed("CP-001", "gone.log", byteArrayOf(1))

        val response = resource.delete("CP-001", storedName)

        assertEquals(204, response.status)
        assertNull(storage.getFile("CP-001", storedName))
    }

    @Test
    fun `delete returns 404 with error body for a missing file`() {
        val response = resource.delete("CP-001", "missing.log")

        assertEquals(404, response.status)
        @Suppress("UNCHECKED_CAST")
        val entity = response.entity as Map<String, String>
        assertEquals("File not found: missing.log", entity["error"])
    }
}
