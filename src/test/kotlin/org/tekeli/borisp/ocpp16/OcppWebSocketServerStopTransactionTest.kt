package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerStopTransactionTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted for valid StopTransaction`() {
        val response = server.onTextMessage("""[2,"stop-1","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("stop-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"stop-2","StopTransaction",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing transactionId`() {
        val response = server.onTextMessage("""[2,"stop-3","StopTransaction",{"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("transactionId is required"))
    }

    @Test
    fun `should return FormationViolation for missing meterStop`() {
        val response = server.onTextMessage("""[2,"stop-4","StopTransaction",{"transactionId":1,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterStop is required"))
    }

    @Test
    fun `should return FormationViolation for meterStop as String`() {
        val response = server.onTextMessage("""[2,"stop-14","StopTransaction",{"transactionId":1,"meterStop":"not-a-number","timestamp":"2024-01-01T01:00:00Z"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterStop must be an integer"))
    }

    @Test
    fun `should return FormationViolation for missing timestamp`() {
        val response = server.onTextMessage("""[2,"stop-5","StopTransaction",{"transactionId":1,"meterStop":5000,"reason":"Local"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should return FormationViolation for empty timestamp`() {
        val response = server.onTextMessage("""[2,"stop-6","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"","reason":"Local"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should accept StopTransaction with missing reason`() {
        val response = server.onTextMessage("""[2,"stop-7","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should accept StopTransaction with empty reason`() {
        val response = server.onTextMessage("""[2,"stop-8","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":""}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for invalid reason`() {
        val response = server.onTextMessage("""[2,"stop-9","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"InvalidReason"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid reason"))
    }

    @Test
    fun `should accept StopTransaction with optional idTag`() {
        val response = server.onTextMessage("""[2,"stop-10","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"TAG123"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"stop-11","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$longIdTag"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should accept StopTransaction with idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"stop-12","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$maxIdTag"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should accept StopTransaction with empty idTag`() {
        val response = server.onTextMessage("""[2,"stop-13","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":""}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should preserve messageId in StopTransaction response`() {
        val response = server.onTextMessage("""[2,"stop-msg-id","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")
        assertTrue(response.contains("stop-msg-id"))
    }
}
