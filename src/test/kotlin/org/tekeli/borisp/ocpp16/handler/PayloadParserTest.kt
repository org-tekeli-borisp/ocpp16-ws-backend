package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException

class PayloadParserTest {

    @Test
    fun `requiredInt accepts valid integer`() {
        val map = mapOf("value" to 42)

        assertEquals(42, map.requiredInt("value"))
    }

    @Test
    fun `requiredInt accepts Long`() {
        val map = mapOf("value" to 42L)

        assertEquals(42, map.requiredInt("value"))
    }

    @Test
    fun `requiredInt accepts Double`() {
        val map = mapOf("value" to 42.0)

        assertEquals(42, map.requiredInt("value"))
    }

    @Test
    fun `requiredInt throws when missing`() {
        val map = mapOf<String, Any>()

        assertThrows(FormationViolationException::class.java) {
            map.requiredInt("value")
        }
    }

    @Test
    fun `requiredInt throws when not a number`() {
        val map = mapOf("value" to "not-a-number")

        assertThrows(FormationViolationException::class.java) {
            map.requiredInt("value")
        }
    }

    @Test
    fun `requiredInt throws when below min`() {
        val map = mapOf("value" to 5)

        assertThrows(FormationViolationException::class.java) {
            map.requiredInt("value", min = 10)
        }
    }

    @Test
    fun `requiredInt throws when above max`() {
        val map = mapOf("value" to 100)

        assertThrows(FormationViolationException::class.java) {
            map.requiredInt("value", max = 50)
        }
    }

    @Test
    fun `requiredInt accepts value at min boundary`() {
        val map = mapOf("value" to 10)

        assertEquals(10, map.requiredInt("value", min = 10))
    }

    @Test
    fun `requiredInt accepts value at max boundary`() {
        val map = mapOf("value" to 50)

        assertEquals(50, map.requiredInt("value", max = 50))
    }

    @Test
    fun `requiredString accepts valid string`() {
        val map = mapOf("value" to "test")

        assertEquals("test", map.requiredString("value", 10))
    }

    @Test
    fun `requiredString throws when missing`() {
        val map = mapOf<String, Any>()

        assertThrows(FormationViolationException::class.java) {
            map.requiredString("value", 10)
        }
    }

    @Test
    fun `requiredString throws when blank`() {
        val map = mapOf("value" to "")

        assertThrows(FormationViolationException::class.java) {
            map.requiredString("value", 10)
        }
    }

    @Test
    fun `requiredString throws when exceeding maxLength`() {
        val map = mapOf("value" to "toolong")

        assertThrows(FormationViolationException::class.java) {
            map.requiredString("value", 3)
        }
    }

    @Test
    fun `optionalString returns null when missing`() {
        val map = mapOf<String, Any>()

        assertNull(map.optionalString("value", 10))
    }

    @Test
    fun `optionalString returns trimmed value`() {
        val map = mapOf("value" to "  test  ")

        assertEquals("test", map.optionalString("value", 10))
    }

    @Test
    fun `optionalString throws when exceeding maxLength`() {
        val map = mapOf("value" to "toolong")

        assertThrows(FormationViolationException::class.java) {
            map.optionalString("value", 3)
        }
    }

    @Test
    fun `requiredInstant accepts valid ISO timestamp`() {
        val map = mapOf("timestamp" to "2024-01-01T00:00:00Z")

        assertEquals("2024-01-01T00:00:00Z", map.requiredInstant("timestamp").toString())
    }

    @Test
    fun `requiredInstant throws when missing`() {
        val map = mapOf<String, Any>()

        assertThrows(FormationViolationException::class.java) {
            map.requiredInstant("timestamp")
        }
    }

    @Test
    fun `requiredInstant throws when invalid format`() {
        val map = mapOf("timestamp" to "not-a-date")

        assertThrows(FormationViolationException::class.java) {
            map.requiredInstant("timestamp")
        }
    }

    @Test
    fun `requiredStringIn accepts valid value`() {
        val map = mapOf("status" to "Accepted")

        assertEquals("Accepted", map.requiredStringIn("status", setOf("Accepted", "Rejected")))
    }

    @Test
    fun `requiredStringIn throws when invalid value`() {
        val map = mapOf("status" to "Invalid")

        assertThrows(FormationViolationException::class.java) {
            map.requiredStringIn("status", setOf("Accepted", "Rejected"))
        }
    }

    @Test
    fun `requiredStringIn throws when missing`() {
        val map = mapOf<String, Any>()

        assertThrows(FormationViolationException::class.java) {
            map.requiredStringIn("status", setOf("Accepted", "Rejected"))
        }
    }
}
