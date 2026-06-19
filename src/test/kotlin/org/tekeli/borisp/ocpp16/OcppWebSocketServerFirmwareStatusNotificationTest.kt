package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerFirmwareStatusNotificationTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return empty CallResult for valid FirmwareStatusNotification`() {
        val response = server.onTextMessage("""[2,"fs-1","FirmwareStatusNotification",{"status":"Downloaded"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("fs-1"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"fs-2","FirmwareStatusNotification",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing status`() {
        val response = server.onTextMessage("""[2,"fs-3","FirmwareStatusNotification",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for empty status`() {
        val response = server.onTextMessage("""[2,"fs-5","FirmwareStatusNotification",{"status":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for invalid status`() {
        val response = server.onTextMessage("""[2,"fs-4","FirmwareStatusNotification",{"status":"InvalidStatus"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }
}
