package org.tekeli.borisp.ocpp16

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

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer =
        org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
}
