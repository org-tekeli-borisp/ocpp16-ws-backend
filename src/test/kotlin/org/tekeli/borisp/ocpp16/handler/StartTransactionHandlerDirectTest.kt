package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import java.time.Instant

class StartTransactionHandlerDirectTest {

    private val handler = StartTransactionHandler()

    // -- connectorId tests --

    @Test
    fun `validatePayload extracts connectorId as 1`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(1, parsed.connectorId)
    }

    @Test
    fun `validatePayload extracts large connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 999999,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(999999, parsed.connectorId)
    }

    @Test
    fun `validatePayload extracts connectorId from Long`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 42L,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42, parsed.connectorId)
    }

    @Test
    fun `validatePayload throws for missing connectorId`() {
        val payload = mapOf<String, Any>(
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for non-integer connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to "not-a-number",
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for zero connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 0,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for negative connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to -5,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    // -- idTag tests --

    @Test
    fun `validatePayload extracts valid idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("RFID001", parsed.idTag)
    }

    @Test
    fun `validatePayload throws for missing idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("idTag"))
    }

    @Test
    fun `validatePayload throws for empty idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("idTag"))
    }

    @Test
    fun `validatePayload throws for idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to longIdTag,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to maxIdTag,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxIdTag, parsed.idTag)
    }

    // -- meterStart tests --

    @Test
    fun `validatePayload extracts valid meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 12345,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(12345, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts meterStart as zero`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts negative meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to -100,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(-100, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts meterStart from Long`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 99999L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(99999, parsed.meterStart)
    }

    @Test
    fun `validatePayload throws for missing meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStart"))
    }

    @Test
    fun `validatePayload throws for non-integer meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to "not-a-number",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStart"))
    }

    // -- startTime (timestamp) tests --

    @Test
    fun `validatePayload extracts valid startTime`() {
        val expected = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to expected.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(expected, parsed.startTime)
    }

    @Test
    fun `validatePayload throws for missing timestamp`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for empty timestamp`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for invalid timestamp format`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "not-a-date"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid timestamp"))
    }

    // -- full payload tests --

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val startTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "connectorId" to 3,
            "idTag" to "RFID999",
            "meterStart" to 5000,
            "timestamp" to startTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(3, parsed.connectorId)
        assertEquals("RFID999", parsed.idTag)
        assertEquals(5000, parsed.meterStart)
        assertEquals(startTime, parsed.startTime)
    }

    @Test
    fun `validatePayload extracts all fields with edge values`() {
        val startTime = Instant.parse("2024-12-31T23:59:59Z")
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "connectorId" to 100000,
            "idTag" to maxIdTag,
            "meterStart" to 999999,
            "timestamp" to startTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(100000, parsed.connectorId)
        assertEquals(maxIdTag, parsed.idTag)
        assertEquals(999999, parsed.meterStart)
        assertEquals(startTime, parsed.startTime)
    }
}
