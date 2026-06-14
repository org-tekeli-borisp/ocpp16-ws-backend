package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException

class BootNotificationHandlerDirectTest {

    private val handler = BootNotificationHandler()

    // -- vendor (chargePointVendor) tests --

    @Test
    fun `validatePayload extracts valid vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
    }

    @Test
    fun `validatePayload throws for missing vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for empty vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "",
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for whitespace vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "   ",
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for vendor exceeding 20 chars`() {
        val longVendor = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to longVendor,
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts vendor exactly 20 chars`() {
        val maxVendor = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to maxVendor,
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxVendor, parsed.vendor)
    }

    // -- model (chargePointModel) tests --

    @Test
    fun `validatePayload extracts valid model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("ModelX", parsed.model)
    }

    @Test
    fun `validatePayload throws for missing model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for empty model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for whitespace model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "   "
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for model exceeding 20 chars`() {
        val longModel = "B".repeat(21)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to longModel
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts model exactly 20 chars`() {
        val maxModel = "B".repeat(20)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to maxModel
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxModel, parsed.model)
    }

    // -- firmwareVersion tests --

    @Test
    fun `validatePayload extracts present firmwareVersion`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX",
            "firmwareVersion" to "1.2.3"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("1.2.3", parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload returns null firmwareVersion when missing`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload treats absent firmwareVersion as null`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.firmwareVersion)
        assertNull(payload["firmwareVersion"])
    }

    // -- extractStringField direct tests --

    @Test
    fun `extractStringField returns valid value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "someValue"
        )

        val result = handler.extractStringField(payload, "fieldName", 50)

        assertEquals("someValue", result)
    }

    @Test
    fun `extractStringField throws for missing field`() {
        val payload = mapOf<String, Any>()

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractStringField(payload, "fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `extractStringField throws for empty value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractStringField(payload, "fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `extractStringField throws for whitespace value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "  "
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractStringField(payload, "fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `extractStringField throws when value exceeds maxLength`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "A".repeat(11)
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractStringField(payload, "fieldName", 10)
        }
        assertTrue(ex.message!!.contains("10 characters"))
    }

    @Test
    fun `extractStringField accepts value exactly at maxLength`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "A".repeat(10)
        )

        val result = handler.extractStringField(payload, "fieldName", 10)

        assertEquals("A".repeat(10), result)
    }

    // -- full validation tests --

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX",
            "firmwareVersion" to "2.0.0"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
        assertEquals("ModelX", parsed.model)
        assertEquals("2.0.0", parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload extracts all fields with optional firmwareVersion missing`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
        assertEquals("ModelX", parsed.model)
        assertNull(parsed.firmwareVersion)
    }
}
