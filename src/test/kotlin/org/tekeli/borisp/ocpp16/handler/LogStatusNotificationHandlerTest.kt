package org.tekeli.borisp.ocpp16

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.command.PayloadValidators
import org.tekeli.borisp.ocpp16.handler.LogStatusNotificationHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class LogStatusNotificationHandlerTest {

    private val handler = LogStatusNotificationHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid LogStatusNotification`() {
        val call = makeCall("LogStatusNotification", mapOf("status" to "Uploading"))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should accept LogStatusNotification with requestId`() {
        val call = makeCall("LogStatusNotification", mapOf(
            "status" to "Uploaded",
            "requestId" to 456
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("LogStatusNotification", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing status`() {
        val call = makeCall("LogStatusNotification", mapOf())

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty status`() {
        val call = makeCall("LogStatusNotification", mapOf("status" to ""))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for invalid status`() {
        val call = makeCall("LogStatusNotification", mapOf("status" to "InvalidStatus"))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept all valid upload log status values`() {
        val validStatuses = listOf(
            "BadMessage", "Idle", "NotSupportedOperation",
            "PermissionDenied", "Uploaded", "UploadFailure", "Uploading"
        )

        for (status in validStatuses) {
            val call = makeCall("LogStatusNotification", mapOf("status" to status))
            val response = handler.handle(call, mockServer())
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    @Test
    fun `should throw FormationViolation for null status`() {
        val call = makeCall("LogStatusNotification", PayloadValidators.safeMap(mapOf("status" to null as Any?)))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("LogStatusNotification", mapOf("status" to "Idle"))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    // == MUTATION KILL TESTS ==

    @Test
    fun `should throw FormationViolation for whitespace-only status - kills isBlank mutant`() {
        // When isBlank() call is removed, whitespace passes the blank check
        val call = makeCall("LogStatusNotification", mapOf("status" to "\t "))

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("status is required", ex.message)
    }

    @Test
    fun `response payload is an empty object - kills emptyMap and requestId mutants`() {
        // When emptyMap() call is removed, payload serializes as null instead of {}
        val call = makeCall("LogStatusNotification", mapOf(
            "status" to "Uploaded",
            "requestId" to 99
        ))

        val response = handler.handle(call, mockServer())

        val mapper = ObjectMapper()
        val jsonNodes = mapper.readValue(response, Array<Any>::class.java)
        assertEquals(3, jsonNodes[0], "Message type should be 3 (CallResult)")
        assertEquals("test-id", jsonNodes[1], "MessageId should be preserved")
        val payload = jsonNodes[2] as Map<String, *>
        assertTrue(payload.isEmpty(), "Response payload should be an empty map, got: $payload")
    }

    @Test
    fun `response messageId matches call messageId exactly - kills messageId mutants`() {
        val uniqueId = "unique-log-status-msg-id"
        val call = OcppMessage.Call(uniqueId, "LogStatusNotification", mapOf("status" to "Uploading"))

        val response = handler.handle(call, mockServer())

        val mapper = ObjectMapper()
        val jsonNodes = mapper.readValue(response, Array<Any>::class.java)
        assertEquals(uniqueId, jsonNodes[1], "Response messageId must match call messageId exactly")
    }

    @Test
    fun `throw FormationViolation with exact message for null payload - kills EQUAL_ELSE on payload null`() {
        val call = makeCall("LogStatusNotification", null)

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("Payload is null", ex.message)
    }

    @Test
    fun `throw FormationViolation with exact message for blank status - kills EQUAL_ELSE on blank`() {
        val call = makeCall("LogStatusNotification", mapOf("status" to ""))

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
        assertEquals("status is required", ex.message)
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer =
        org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
}
