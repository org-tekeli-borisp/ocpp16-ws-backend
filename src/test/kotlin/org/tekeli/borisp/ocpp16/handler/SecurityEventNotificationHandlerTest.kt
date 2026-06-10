package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.SecurityEventNotificationHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class SecurityEventNotificationHandlerTest {

    private val handler = SecurityEventNotificationHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid SecurityEventNotification`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should accept SecurityEventNotification with techInfo`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "InvalidTLSVersion",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to "TLS 1.0 rejected"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("SecurityEventNotification", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing timestamp`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty timestamp`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to ""
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for type exceeding 50 characters`() {
        val longType = "A".repeat(51)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to longType,
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept type with exactly 50 characters`() {
        val maxType = "A".repeat(50)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to maxType,
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should throw FormationViolation for techInfo exceeding 255 characters`() {
        val longTechInfo = "A".repeat(256)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to longTechInfo
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept techInfo with exactly 255 characters`() {
        val maxTechInfo = "A".repeat(255)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to maxTechInfo
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should accept all valid security event types`() {
        val validEvents = listOf(
            "FirmwareUpdated", "FirmwareVerificationFailed",
            "InvalidChargePointCertificate", "InvalidCentralSystemCertificate",
            "InvalidTLSCipherSuite", "InvalidTLSVersion",
            "LocalAccess", "ResetFailed", "Reset", "Tampering",
            "TransactionInfoNotStored", "InvalidFirmwareSigningCertificate",
            "InvalidFirmwareSignature", "DiscardedRenewedClientCertificate",
            "UnauthorizedAccess"
        )

        for (eventType in validEvents) {
            val call = makeCall("SecurityEventNotification", mapOf(
                "type" to eventType,
                "timestamp" to "2024-01-01T00:00:00Z"
            ))

            val response = handler.handle(call, mockServer())
            assertTrue(response.startsWith("[3,"), "Should accept event type: $eventType")
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should handle null type field`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to null as Any?,
            "timestamp" to "2024-01-01T00:00:00Z"
        ) as Map<String, Any>)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should handle whitespace-only type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "   ",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer {
        val server = org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
        server.chargePointId = "CP-001"
        return server
    }
}
