package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerAuthorizeTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted for valid Authorize`() {
        val response = server.onTextMessage("""[2,"auth-1","Authorize",{"idTag":"ABC123"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("auth-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"auth-2","Authorize",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing idTag`() {
        val response = server.onTextMessage("""[2,"auth-3","Authorize",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for empty idTag`() {
        val response = server.onTextMessage("""[2,"auth-4","Authorize",{"idTag":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"auth-5","Authorize",{"idTag":"$longIdTag"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should accept Authorize with idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"auth-6","Authorize",{"idTag":"$maxIdTag"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should preserve messageId in Authorize response`() {
        val response = server.onTextMessage("""[2,"custom-msg-id","Authorize",{"idTag":"TAG001"}]""")
        assertTrue(response.contains("custom-msg-id"))
    }
}
