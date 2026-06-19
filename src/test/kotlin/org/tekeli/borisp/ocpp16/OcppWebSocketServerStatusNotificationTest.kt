package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerStatusNotificationTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return empty CallResult for valid StatusNotification`() {
        val response = server.onTextMessage("""[2,"sn-1","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("sn-1"))
        assertTrue(response.contains("{}"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"sn-2","StatusNotification",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing connectorId`() {
        val response = server.onTextMessage("""[2,"sn-3","StatusNotification",{"errorCode":"NoError","status":"Available"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for missing errorCode`() {
        val response = server.onTextMessage("""[2,"sn-4","StatusNotification",{"connectorId":1,"status":"Available"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("errorCode is required"))
    }

    @Test
    fun `should return FormationViolation for empty errorCode`() {
        val response = server.onTextMessage("""[2,"sn-5","StatusNotification",{"connectorId":1,"errorCode":"","status":"Available"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("errorCode is required"))
    }

    @Test
    fun `should return FormationViolation for invalid errorCode`() {
        val response = server.onTextMessage("""[2,"sn-6","StatusNotification",{"connectorId":1,"errorCode":"InvalidError","status":"Available"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid errorCode"))
    }

    @Test
    fun `should return FormationViolation for missing status`() {
        val response = server.onTextMessage("""[2,"sn-7","StatusNotification",{"connectorId":1,"errorCode":"NoError"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for empty status`() {
        val response = server.onTextMessage("""[2,"sn-8","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for invalid status`() {
        val response = server.onTextMessage("""[2,"sn-9","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"InvalidStatus"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }

    @Test
    fun `should accept StatusNotification with optional info field`() {
        val response = server.onTextMessage("""[2,"sn-10","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"Connector OK"}]""")
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for info exceeding 50 characters`() {
        val longInfo = "A".repeat(51)
        val response = server.onTextMessage("""[2,"sn-11","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"$longInfo"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("info must not exceed 50 characters"))
    }

    @Test
    fun `should accept StatusNotification with info exactly 50 characters`() {
        val maxInfo = "A".repeat(50)
        val response = server.onTextMessage("""[2,"sn-12","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"$maxInfo"}]""")
        assertTrue(response.startsWith("[3,"))
    }
}
