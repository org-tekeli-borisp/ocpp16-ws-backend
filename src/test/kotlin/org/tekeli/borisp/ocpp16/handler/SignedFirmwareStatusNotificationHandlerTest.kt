package org.tekeli.borisp.ocpp16

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.SignedFirmwareStatusNotificationHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class SignedFirmwareStatusNotificationHandlerTest {

    private val handler = SignedFirmwareStatusNotificationHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid SignedFirmwareStatusNotification`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf(
            "status" to "Downloading"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should accept SignedFirmwareStatusNotification with requestId`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf(
            "status" to "Downloaded",
            "requestId" to 123
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("SignedFirmwareStatusNotification", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing status`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf())

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty status`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to ""))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for invalid status`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to "InvalidStatus"))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept all valid firmware status values`() {
        val validStatuses = listOf(
            "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
            "DownloadPaused", "Idle", "InstallationFailed", "Installing",
            "Installed", "InstallRebooting", "InstallScheduled",
            "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
        )

        for (status in validStatuses) {
            val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to status))
            val response = handler.handle(call, mockServer())
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    @Test
    fun `should throw FormationViolation for null status`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to null as Any?) as Map<String, Any>)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to "Idle"))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    // == MUTATION KILL TESTS ==

    @Test
    fun `should throw FormationViolation for whitespace-only status - kills isBlank mutant`() {
        // When isBlank() call is removed, "  ".isBlank always returns false,
        // so whitespace status passes the blank check and hits the validStatuses check,
        // throwing a DIFFERENT error message containing "Invalid status"
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to "   "))

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("status is required", ex.message)
    }

    @Test
    fun `response payload is an empty object - kills emptyMap and requestId mutants`() {
        // When emptyMap() call is removed, payload becomes null and serializes differently
        // When Map::get for requestId is removed, the response format may change
        val call = makeCall("SignedFirmwareStatusNotification", mapOf(
            "status" to "Installed",
            "requestId" to 42
        ))

        val response = handler.handle(call, mockServer())

        // Parse the JSON array: [3, "test-id", {}]
        val mapper = ObjectMapper()
        val jsonNodes = mapper.readValue(response, Array<Any>::class.java)
        assertEquals(3, jsonNodes[0], "Message type should be 3 (CallResult)")
        assertEquals("test-id", jsonNodes[1], "MessageId should be preserved")
        val payload = jsonNodes[2] as Map<String, *>
        assertTrue(payload.isEmpty(), "Response payload should be an empty map, got: $payload")
    }

    @Test
    fun `response messageId matches call messageId exactly - kills messageId mutants`() {
        val uniqueId = "unique-signed-fw-msg-id"
        val call = OcppMessage.Call(uniqueId, "SignedFirmwareStatusNotification", mapOf("status" to "Idle"))

        val response = handler.handle(call, mockServer())

        val mapper = ObjectMapper()
        val jsonNodes = mapper.readValue(response, Array<Any>::class.java)
        assertEquals(uniqueId, jsonNodes[1], "Response messageId must match call messageId exactly")
    }

    @Test
    fun `throw FormationViolation with exact message for null payload - kills EQUAL_ELSE on payload null`() {
        val call = makeCall("SignedFirmwareStatusNotification", null)

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("Payload is null", ex.message)
    }

    @Test
    fun `throw FormationViolation with exact message for blank status - kills EQUAL_ELSE on blank`() {
        val call = makeCall("SignedFirmwareStatusNotification", mapOf("status" to ""))

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("status is required", ex.message)
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer =
        org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
}
