package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import java.time.Instant

class StopTransactionHandlerDirectTest {

    private val handler = StopTransactionHandler()

    @Test
    fun `validatePayload extracts transactionId correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 42L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42L, parsed.transactionId)
    }

    @Test
    fun `validatePayload extracts transactionId from Int`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 42,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42L, parsed.transactionId)
    }

    @Test
    fun `validatePayload throws for missing transactionId`() {
        val payload = mapOf<String, Any>(
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("transactionId"))
    }

    @Test
    fun `validatePayload throws for non-integer transactionId`() {
        val payload = mapOf<String, Any>(
            "transactionId" to "not-a-number",
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("transactionId"))
    }

    @Test
    fun `validatePayload extracts meterStop correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 9999,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(9999, parsed.meterStop)
    }

    @Test
    fun `validatePayload throws for missing meterStop`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStop"))
    }

    @Test
    fun `validatePayload extracts stopTime correctly`() {
        val expectedTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to expectedTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(expectedTime, parsed.stopTime)
    }

    @Test
    fun `validatePayload throws for missing timestamp`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for empty timestamp`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
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
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "not-a-date"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid timestamp"))
    }

    @Test
    fun `validatePayload returns null reason when missing`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.reason)
    }

    @Test
    fun `validatePayload returns null reason when empty`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to ""
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.reason)
    }

    @Test
    fun `validatePayload extracts valid reason`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to "Local"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("Local", parsed.reason)
    }

    @Test
    fun `validatePayload extracts each valid reason`() {
        val validReasons = listOf(
            "DeAuthorized", "EmergencyStop", "EVDisconnected", "HardReset",
            "Local", "Other", "PowerLoss", "Reboot", "Remote", "SoftReset",
            "UnlockCommand"
        )
        for (reason in validReasons) {
            val payload = mapOf<String, Any>(
                "transactionId" to 1L,
                "meterStop" to 5000,
                "timestamp" to "2024-01-01T00:00:00Z",
                "reason" to reason
            )
            val parsed = handler.validatePayload(payload)
            assertEquals(reason, parsed.reason, "Reason $reason should be valid")
        }
    }

    @Test
    fun `validatePayload throws for invalid reason`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to "InvalidReason"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid reason"))
    }

    @Test
    fun `validatePayload returns null idTagEnd when missing`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.idTagEnd)
    }

    @Test
    fun `validatePayload returns null idTagEnd when empty`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to ""
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.idTagEnd)
    }

    @Test
    fun `validatePayload extracts idTagEnd correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to "ABC123"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("ABC123", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload trims idTagEnd`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to "  TAG  "
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("TAG", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload throws for idTagEnd exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to longIdTag
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts idTagEnd exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to maxIdTag
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxIdTag, parsed.idTagEnd)
    }

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val stopTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "transactionId" to 777L,
            "meterStop" to 12345,
            "timestamp" to stopTime.toString(),
            "reason" to "EVDisconnected",
            "idTag" to "END_TAG"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(777L, parsed.transactionId)
        assertEquals(12345, parsed.meterStop)
        assertEquals(stopTime, parsed.stopTime)
        assertEquals("EVDisconnected", parsed.reason)
        assertEquals("END_TAG", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload handles meterStop as Long`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(5000, parsed.meterStop)
    }

    @Test
    fun `validatePayload handles transactionId as Int`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 99,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(99L, parsed.transactionId)
    }

    @Test
    fun `validatePayload handles transactionId 0`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 0L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0L, parsed.transactionId)
    }

    @Test
    fun `validatePayload handles meterStop 0`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0, parsed.meterStop)
    }
}
