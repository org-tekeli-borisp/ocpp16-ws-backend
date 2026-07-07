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
}
