package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class BootNotificationHandlerBranchTest {

    private val handler = BootNotificationHandler()

    @Test
    fun `validatePayload accepts valid payload with firmwareVersion`() {
        val payload = mapOf(
            "chargePointVendor" to "Vendor",
            "chargePointModel" to "Model",
            "firmwareVersion" to "1.0.0"
        )

        val result = handler.validatePayload(payload)

        assertEquals("Vendor", result.vendor)
        assertEquals("Model", result.model)
        assertEquals("1.0.0", result.firmwareVersion)
    }

    @Test
    fun `validatePayload accepts payload without firmwareVersion`() {
        val payload = mapOf(
            "chargePointVendor" to "Vendor",
            "chargePointModel" to "Model"
        )

        val result = handler.validatePayload(payload)

        assertEquals("Vendor", result.vendor)
        assertEquals("Model", result.model)
        assertNull(result.firmwareVersion)
    }

    @Test
    fun `validatePayload rejects missing vendor`() {
        val payload = mapOf(
            "chargePointModel" to "Model"
        )

        assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
    }

    @Test
    fun `validatePayload rejects missing model`() {
        val payload = mapOf(
            "chargePointVendor" to "Vendor"
        )

        assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
    }

    @Test
    fun `validatePayload rejects vendor exceeding max length`() {
        val payload = mapOf(
            "chargePointVendor" to "A".repeat(OcppConstants.MAX_VENDOR_LENGTH + 1),
            "chargePointModel" to "Model"
        )

        assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
    }

    @Test
    fun `validatePayload rejects model exceeding max length`() {
        val payload = mapOf(
            "chargePointVendor" to "Vendor",
            "chargePointModel" to "A".repeat(OcppConstants.MAX_MODEL_LENGTH + 1)
        )

        assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
    }
}
