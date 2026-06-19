package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerDataTransferTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted for valid DataTransfer`() {
        val response = server.onTextMessage("""[2,"dt-1","DataTransfer",{"vendorId":"VendorX"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("dt-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"dt-2","DataTransfer",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing vendorId`() {
        val response = server.onTextMessage("""[2,"dt-3","DataTransfer",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId is required"))
    }

    @Test
    fun `should return FormationViolation for empty vendorId`() {
        val response = server.onTextMessage("""[2,"dt-4","DataTransfer",{"vendorId":""}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId is required"))
    }

    @Test
    fun `should return FormationViolation for vendorId exceeding 255 characters`() {
        val longVendorId = "A".repeat(256)
        val response = server.onTextMessage("""[2,"dt-5","DataTransfer",{"vendorId":"$longVendorId"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId must not exceed 255 characters"))
    }

    @Test
    fun `should accept DataTransfer with vendorId exactly 255 characters`() {
        val maxVendorId = "A".repeat(255)
        val response = server.onTextMessage("""[2,"dt-6","DataTransfer",{"vendorId":"$maxVendorId"}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should accept DataTransfer with optional messageId and data`() {
        val response = server.onTextMessage("""[2,"dt-7","DataTransfer",{"vendorId":"VendorX","messageId":"diagStart","data":"dGVzdA=="}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should accept DataTransfer with empty messageId`() {
        val response = server.onTextMessage("""[2,"dt-8","DataTransfer",{"vendorId":"VendorX","messageId":""}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }
}
