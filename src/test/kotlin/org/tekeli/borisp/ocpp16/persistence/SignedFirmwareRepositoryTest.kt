package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@jakarta.transaction.Transactional
class SignedFirmwareRepositoryTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM signed_firmware").executeUpdate()
        em.flush()
    }

    @Test
    fun `should persist and find signed firmware by request id`() {
        val firmware = persistenceService.createSignedFirmware(
            chargePointId = "CP-001",
            requestId = 123,
            location = "https://example.com/fw.bin",
            retrieveDateTime = Instant.parse("2024-01-01T00:00:00Z"),
            installDateTime = null,
            signingCertificate = "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
            signature = "deadbeef"
        )

        assertNotNull(firmware.id)
        assertEquals("CP-001", firmware.chargePointId)
        assertEquals(123, firmware.requestId)
        assertEquals("Accepted", firmware.status)

        val found = persistenceService.findSignedFirmwareByRequestId("CP-001", 123)
        assertNotNull(found)
        assertEquals("https://example.com/fw.bin", found!!.location)
    }

    @Test
    fun `should return null for non-existent request id`() {
        val found = persistenceService.findSignedFirmwareByRequestId("CP-001", 999)
        assertNull(found)
    }

    @Test
    fun `should update firmware status`() {
        persistenceService.createSignedFirmware(
            chargePointId = "CP-001",
            requestId = 456,
            location = "https://example.com/fw.bin",
            retrieveDateTime = Instant.parse("2024-01-01T00:00:00Z"),
            installDateTime = null,
            signingCertificate = "cert",
            signature = "sig"
        )
        em.flush()

        val updated = persistenceService.updateSignedFirmwareStatus("CP-001", 456, "Downloading")
        assertTrue(updated)

        val found = persistenceService.findSignedFirmwareByRequestId("CP-001", 456)
        assertNotNull(found)
        assertEquals("Downloading", found!!.status)
    }

    @Test
    fun `should return false when updating non-existent firmware`() {
        val updated = persistenceService.updateSignedFirmwareStatus("CP-001", 999, "Downloading")
        assertFalse(updated)
    }

    @Test
    fun `should find all signed firmware for charge point`() {
        persistenceService.createSignedFirmware("CP-001", 1, "url1", Instant.parse("2024-01-01T00:00:00Z"), null, "cert1", "sig1")
        persistenceService.createSignedFirmware("CP-001", 2, "url2", Instant.parse("2024-01-01T01:00:00Z"), null, "cert2", "sig2")
        persistenceService.createSignedFirmware("CP-002", 3, "url3", Instant.parse("2024-01-01T02:00:00Z"), null, "cert3", "sig3")
        em.flush()

        val all = persistenceService.findAllSignedFirmware("CP-001")
        assertEquals(2, all.size)

        val allCp2 = persistenceService.findAllSignedFirmware("CP-002")
        assertEquals(1, allCp2.size)
    }

    @Test
    fun `should find signed firmware by id`() {
        val firmware = persistenceService.createSignedFirmware(
            chargePointId = "CP-001",
            requestId = 789,
            location = "https://example.com/fw.bin",
            retrieveDateTime = Instant.parse("2024-01-01T00:00:00Z"),
            installDateTime = null,
            signingCertificate = "cert",
            signature = "sig"
        )
        em.flush()

        val found = persistenceService.findSignedFirmwareById(firmware.id ?: -1L)
        assertNotNull(found)
        assertEquals(789, found!!.requestId)
    }

    @Test
    fun `should return null for non-existent firmware id`() {
        val found = persistenceService.findSignedFirmwareById(99999L)
        assertNull(found)
    }

    @Test
    fun `should create signed firmware with installDateTime`() {
        val installTime = Instant.parse("2024-01-02T00:00:00Z")
        val firmware = persistenceService.createSignedFirmware(
            chargePointId = "CP-001",
            requestId = 100,
            location = "https://example.com/fw.bin",
            retrieveDateTime = Instant.parse("2024-01-01T00:00:00Z"),
            installDateTime = installTime,
            signingCertificate = "cert",
            signature = "sig"
        )

        assertEquals(installTime, firmware.installDateTime)
    }

    @Test
    fun `should support all firmware status values`() {
        val statuses = listOf(
            "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
            "DownloadPaused", "Idle", "InstallationFailed", "Installing",
            "Installed", "InstallRebooting", "InstallScheduled",
            "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
        )

        persistenceService.createSignedFirmware("CP-001", 200, "url", Instant.now(), null, "cert", "sig")
        em.flush()

        for (status in statuses) {
            persistenceService.updateSignedFirmwareStatus("CP-001", 200, status)
            val found = persistenceService.findSignedFirmwareByRequestId("CP-001", 200)
            assertEquals(status, found?.status, "Status should be $status")
        }
    }

    @Test
    fun `should handle different chargePointId for same requestId`() {
        persistenceService.createSignedFirmware("CP-001", 1, "url1", Instant.now(), null, "cert1", "sig1")
        persistenceService.createSignedFirmware("CP-002", 1, "url2", Instant.now(), null, "cert2", "sig2")
        em.flush()

        val cp1 = persistenceService.findSignedFirmwareByRequestId("CP-001", 1)
        val cp2 = persistenceService.findSignedFirmwareByRequestId("CP-002", 1)

        assertNotNull(cp1)
        assertNotNull(cp2)
        assertEquals("url1", cp1!!.location)
        assertEquals("url2", cp2!!.location)
        assertNotEquals(cp1.id, cp2.id)
    }

    @Test
    fun `should return empty list for charge point with no firmware`() {
        val all = persistenceService.findAllSignedFirmware("CP-NONEXISTENT")
        assertTrue(all.isEmpty())
    }

    @Test
    fun `should use default values for unsigned firmware`() {
        val firmware = SignedFirmware()
        assertEquals(0, firmware.requestId)
        assertEquals("Idle", firmware.status)
        assertEquals("", firmware.location)
        assertEquals(Instant.EPOCH, firmware.retrieveDateTime)
    }
}
