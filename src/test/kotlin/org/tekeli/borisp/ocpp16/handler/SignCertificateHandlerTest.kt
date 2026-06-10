package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.SignCertificateHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class SignCertificateHandlerTest {

    private val handler = SignCertificateHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid SignCertificate`() {
        val call = makeCall("SignCertificate", mapOf(
            "csr" to "-----BEGIN CERTIFICATE REQUEST-----\ntest\n-----END CERTIFICATE REQUEST-----"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("SignCertificate", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing csr`() {
        val call = makeCall("SignCertificate", mapOf())

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty csr`() {
        val call = makeCall("SignCertificate", mapOf("csr" to ""))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for csr exceeding 5500 characters`() {
        val longCsr = "A".repeat(5501)
        val call = makeCall("SignCertificate", mapOf("csr" to longCsr))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept csr with exactly 5500 characters`() {
        val maxCsr = "A".repeat(5500)
        val call = makeCall("SignCertificate", mapOf("csr" to maxCsr))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should throw FormationViolation for null csr`() {
        val call = makeCall("SignCertificate", mapOf("csr" to null as Any?) as Map<String, Any>)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for whitespace-only csr`() {
        val call = makeCall("SignCertificate", mapOf("csr" to "   "))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("SignCertificate", mapOf("csr" to "test-csr"))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should always return Accepted for valid CSR`() {
        val csr = "-----BEGIN CERTIFICATE REQUEST-----\nMIIB...test...data\n-----END CERTIFICATE REQUEST-----"
        val call = makeCall("SignCertificate", mapOf("csr" to csr))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("Accepted"))
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer =
        org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
}
