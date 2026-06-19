package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerBootNotificationTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted for valid BootNotification`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("123"))
        assertTrue(response.contains("currentTime"))
        assertTrue(response.contains("Accepted"))
        assertFalse(response.contains("FormationViolation"))
    }

    @Test
    fun `should return FormationViolation for empty vendor`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should return FormationViolation for whitespace-only vendor`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"   ","chargePointModel":"Model"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should return FormationViolation for null vendor`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":null,"chargePointModel":"Model"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should return FormationViolation for missing vendor`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointModel":"Model"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should return FormationViolation for empty model`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
        assertFalse(response.contains("Payload validation failed"))
    }

    @Test
    fun `should return FormationViolation for whitespace-only model`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"   "}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should return FormationViolation for null model`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":null}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should return FormationViolation for missing model`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should return FormationViolation for vendor exceeding 20 characters`() {
        val longVendor = "A".repeat(21)
        val response = server.onTextMessage("""[2,"bn-1","BootNotification",{"chargePointVendor":"$longVendor","chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor must not exceed 20 characters"))
        assertFalse(response.contains("Payload validation failed"))
    }

    @Test
    fun `should accept BootNotification with vendor exactly 20 characters`() {
        val maxVendor = "A".repeat(20)
        val response = server.onTextMessage("""[2,"bn-2","BootNotification",{"chargePointVendor":"$maxVendor","chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for model exceeding 20 characters`() {
        val longModel = "A".repeat(21)
        val response = server.onTextMessage("""[2,"bn-3","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"$longModel"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel must not exceed 20 characters"))
    }

    @Test
    fun `should accept BootNotification with model exactly 20 characters`() {
        val maxModel = "A".repeat(20)
        val response = server.onTextMessage("""[2,"bn-4","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"$maxModel"}]""")
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
        assertFalse(response.contains("Payload validation failed"))
    }

    @Test
    fun `should return FormationViolation for empty payload`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should preserve messageId in FormationViolation response`() {
        val response = server.onTextMessage("""[2,"vendor-error-id","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")
        assertTrue(response.contains("vendor-error-id"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should use specific error message not default for FormationViolation`() {
        val response = server.onTextMessage("""[2,"e1","BootNotification",{"chargePointVendor":"","chargePointModel":"M"}]""")
        assertTrue(response.contains("chargePointVendor is required"))
        assertFalse(response.contains("Payload validation failed"))
    }
}
