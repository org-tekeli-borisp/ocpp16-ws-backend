package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class DiagnosticsConfigTest {

    @Inject
    lateinit var diagnosticsConfig: DiagnosticsConfig

    @Test
    fun `should have default upload dir not null`() {
        val uploadDir = diagnosticsConfig.uploadDir()
        assertNotNull(uploadDir)
        assertTrue(uploadDir.isNotBlank())
    }

    @Test
    fun `should have default max file size 100MB`() {
        assertEquals(100 * 1024 * 1024L, diagnosticsConfig.maxFileSizeBytes())
    }

    @Test
    fun `should have default retention days 30`() {
        assertEquals(30, diagnosticsConfig.retentionDays())
    }

    @Test
    fun `should have default preferred protocol sftp`() {
        assertEquals("sftp", diagnosticsConfig.preferredProtocol())
    }
}
