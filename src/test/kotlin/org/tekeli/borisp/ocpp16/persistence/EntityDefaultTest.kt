package org.tekeli.borisp.ocpp16.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class SecurityLogEntityTest {

    @Test
    fun `constructor defaults`() {
        val log = SecurityLog()

        assertEquals("", log.chargePointId)
        assertEquals("", log.type)
        assertNull(log.techInfo)
    }

    @Test
    fun `constructor with all params`() {
        val timestamp = Instant.now()
        val log = SecurityLog(
            chargePointId = "CP-001",
            type = "SecurityEvent",
            timestamp = timestamp,
            techInfo = "details"
        )

        assertEquals("CP-001", log.chargePointId)
        assertEquals("SecurityEvent", log.type)
        assertEquals(timestamp, log.timestamp)
        assertEquals("details", log.techInfo)
    }

    @Test
    fun `id is null and createdAt is set before persist`() {
        val log = SecurityLog()

        assertNull(log.id)
        assertNotNull(log.createdAt)
    }

    @Test
    fun `setters update all fields`() {
        val log = SecurityLog()
        val ts = Instant.parse("2024-07-07T07:07:07Z")

        log.chargePointId = "CP-002"
        log.type = "CertificateSigned"
        log.timestamp = ts
        log.techInfo = "updated"
        log.createdAt = ts

        assertEquals("CP-002", log.chargePointId)
        assertEquals("CertificateSigned", log.type)
        assertEquals(ts, log.timestamp)
        assertEquals("updated", log.techInfo)
        assertEquals(ts, log.createdAt)
    }
}

class SignedFirmwareEntityTest {

    @Test
    fun `constructor defaults`() {
        val fw = SignedFirmware()

        assertEquals("", fw.chargePointId)
        assertEquals(0, fw.requestId)
        assertEquals("", fw.location)
        assertEquals(Instant.EPOCH, fw.retrieveDateTime)
        assertNull(fw.installDateTime)
        assertEquals("", fw.signingCertificate)
        assertEquals("", fw.signature)
        assertEquals("Idle", fw.status)
    }

    @Test
    fun `constructor with all params`() {
        val fw = SignedFirmware(
            chargePointId = "CP-001",
            requestId = 123,
            location = "https://example.com/fw",
            retrieveDateTime = Instant.parse("2024-01-01T00:00:00Z"),
            installDateTime = Instant.parse("2024-01-02T00:00:00Z"),
            signingCertificate = "cert",
            signature = "sig",
            status = "Accepted"
        )

        assertEquals("CP-001", fw.chargePointId)
        assertEquals(123, fw.requestId)
        assertEquals("Accepted", fw.status)
    }

    @Test
    fun `id is null and createdAt is set before persist`() {
        val fw = SignedFirmware()

        assertNull(fw.id)
        assertNotNull(fw.createdAt)
    }

    @Test
    fun `setters update all fields`() {
        val fw = SignedFirmware()
        val retrieve = Instant.parse("2024-08-08T08:08:08Z")
        val install = Instant.parse("2024-08-09T09:09:09Z")

        fw.chargePointId = "CP-002"
        fw.requestId = 456
        fw.location = "https://example.com/fw2"
        fw.retrieveDateTime = retrieve
        fw.installDateTime = install
        fw.signingCertificate = "cert2"
        fw.signature = "sig2"
        fw.status = "Downloaded"
        fw.createdAt = install

        assertEquals("CP-002", fw.chargePointId)
        assertEquals(456, fw.requestId)
        assertEquals("https://example.com/fw2", fw.location)
        assertEquals(retrieve, fw.retrieveDateTime)
        assertEquals(install, fw.installDateTime)
        assertEquals("cert2", fw.signingCertificate)
        assertEquals("sig2", fw.signature)
        assertEquals("Downloaded", fw.status)
        assertEquals(install, fw.createdAt)
    }
}
