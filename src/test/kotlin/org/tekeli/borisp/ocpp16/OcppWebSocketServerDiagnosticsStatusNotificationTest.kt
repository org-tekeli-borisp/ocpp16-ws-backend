package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerDiagnosticsStatusNotificationTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return empty CallResult for valid DiagnosticsStatusNotification`() {
        val response = server.onTextMessage("""[2,"ds-1","DiagnosticsStatusNotification",{"status":"Uploaded"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("ds-1"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"ds-2","DiagnosticsStatusNotification",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing status`() {
        val response = server.onTextMessage("""[2,"ds-3","DiagnosticsStatusNotification",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for empty status`() {
        val response = server.onTextMessage("""[2,"ds-5","DiagnosticsStatusNotification",{"status":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for invalid status`() {
        val response = server.onTextMessage("""[2,"ds-4","DiagnosticsStatusNotification",{"status":"InvalidStatus"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }
}
