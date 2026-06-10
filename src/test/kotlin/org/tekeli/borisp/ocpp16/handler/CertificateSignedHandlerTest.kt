package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.CertificateSignedHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class CertificateSignedHandlerTest {

    private val handler = CertificateSignedHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid CertificateSigned`() {
        val call = makeCall("CertificateSigned", mapOf(
            "certificateChain" to "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("CertificateSigned", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing certificateChain`() {
        val call = makeCall("CertificateSigned", mapOf())

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty certificateChain`() {
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to ""))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for certificateChain exceeding 10000 characters`() {
        val longChain = "A".repeat(10001)
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to longChain))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept certificateChain with exactly 10000 characters`() {
        val maxChain = "A".repeat(10000)
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to maxChain))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should throw FormationViolation for null certificateChain`() {
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to null as Any?) as Map<String, Any>)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for whitespace-only certificateChain`() {
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to "   "))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to "test-cert"))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should always return Accepted for valid certificateChain`() {
        val cert = "-----BEGIN CERTIFICATE-----\nMIIB...test...data\n-----END CERTIFICATE-----"
        val call = makeCall("CertificateSigned", mapOf("certificateChain" to cert))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("Accepted"))
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer =
        org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
}
