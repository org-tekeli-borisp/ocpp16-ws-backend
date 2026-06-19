package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerStartTransactionTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted with transactionId for valid StartTransaction`() {
        val response = server.onTextMessage("""[2,"st-1","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("st-1"))
        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("transactionId"))
    }

    @Test
    fun `should return transactionId greater than 0 for StartTransaction`() {
        val response = server.onTextMessage("""[2,"st-11","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.contains("\"transactionId\":1"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"st-2","StartTransaction",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing connectorId`() {
        val response = server.onTextMessage("""[2,"st-3","StartTransaction",{"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for missing idTag`() {
        val response = server.onTextMessage("""[2,"st-4","StartTransaction",{"connectorId":1,"meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for empty idTag`() {
        val response = server.onTextMessage("""[2,"st-5","StartTransaction",{"connectorId":1,"idTag":"","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"st-6","StartTransaction",{"connectorId":1,"idTag":"$longIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should accept StartTransaction with idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"st-12","StartTransaction",{"connectorId":1,"idTag":"$maxIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for missing meterStart`() {
        val response = server.onTextMessage("""[2,"st-7","StartTransaction",{"connectorId":1,"idTag":"ABC123","timestamp":"2024-01-01T00:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterStart is required"))
    }

    @Test
    fun `should return FormationViolation for missing timestamp`() {
        val response = server.onTextMessage("""[2,"st-8","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should return FormationViolation for empty timestamp`() {
        val response = server.onTextMessage("""[2,"st-9","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should accept StartTransaction with optional reservationId`() {
        val response = server.onTextMessage("""[2,"st-10","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z","reservationId":42}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }
}
