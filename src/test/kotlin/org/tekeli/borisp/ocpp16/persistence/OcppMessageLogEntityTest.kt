package org.tekeli.borisp.ocpp16.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class OcppMessageLogEntityTest {

    @Test
    fun `constructor with defaults`() {
        val log = OcppMessageLog()

        assertEquals("", log.chargePointId)
        assertEquals("INBOUND", log.direction)
        assertEquals("CALL", log.messageType)
        assertEquals("", log.action)
        assertEquals("", log.messageId)
        assertNull(log.payload)
    }

    @Test
    fun `constructor with all params`() {
        val now = Instant.now()
        val log = OcppMessageLog(
            chargePointId = "CP-001",
            direction = "OUTBOUND",
            messageType = "CALLRESULT",
            action = "Reset",
            messageId = "msg-123",
            payload = """{"type":"Soft"}""",
            timestamp = now
        )

        assertEquals("CP-001", log.chargePointId)
        assertEquals("OUTBOUND", log.direction)
        assertEquals("CALLRESULT", log.messageType)
        assertEquals("Reset", log.action)
        assertEquals("msg-123", log.messageId)
        assertEquals("""{"type":"Soft"}""", log.payload)
        assertEquals(now, log.timestamp)
    }

    @Test
    fun `payload is nullable`() {
        val log = OcppMessageLog(
            chargePointId = "CP-001",
            direction = "INBOUND"
        )

        assertNull(log.payload)
    }

    @Test
    fun `id is null before persist`() {
        assertNull(OcppMessageLog().id)
    }

    @Test
    fun `setters update all fields`() {
        val log = OcppMessageLog()
        val ts = Instant.parse("2024-05-05T05:05:05Z")

        log.chargePointId = "CP-002"
        log.direction = "OUTBOUND"
        log.messageType = "CALLERROR"
        log.action = "BootNotification"
        log.messageId = "msg-456"
        log.payload = """{"code":"NotSupported"}"""
        log.timestamp = ts

        assertEquals("CP-002", log.chargePointId)
        assertEquals("OUTBOUND", log.direction)
        assertEquals("CALLERROR", log.messageType)
        assertEquals("BootNotification", log.action)
        assertEquals("msg-456", log.messageId)
        assertEquals("""{"code":"NotSupported"}""", log.payload)
        assertEquals(ts, log.timestamp)
    }
}
