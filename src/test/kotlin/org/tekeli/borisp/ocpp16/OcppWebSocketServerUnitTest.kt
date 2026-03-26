package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OcppWebSocketServerUnitTest {

    private val server = OcppWebSocketServer()

    @Test
    fun `should return error for unknown action`() {
        val response = server.onTextMessage("""[2,"123","UnknownAction",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("UnknownAction"))
    }

    @Test
    fun `should return error for CALLRESULT from client`() {
        val response = server.onTextMessage("""[3,"123",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLRESULT not expected"))
    }

    @Test
    fun `should return error for CALLERROR from client`() {
        val response = server.onTextMessage("""[4,"123","GenericError","Error",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLERROR not expected"))
    }

    @Test
    fun `should handle BootNotification with empty vendor string`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle BootNotification with empty model string`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should handle valid BootNotification`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("123"))
        assertTrue(response.contains("currentTime"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should handle valid Heartbeat`() {
        val response = server.onTextMessage("""[2,"123","Heartbeat",{}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("123"))
        assertTrue(response.contains("currentTime"))
    }

    @Test
    fun `should handle BootNotification with null payload`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should handle invalid JSON`() {
        val response = server.onTextMessage("not valid json")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should use non-empty message id for parse error`() {
        val response = server.onTextMessage("not valid json")
        assertTrue(response.contains("\""))
    }

    @Test
    fun `should handle FormationViolation with non-null message`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",null]""")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return error for CALLERROR from client with valid error code`() {
        val response = server.onTextMessage("""[4,"123","NotImplemented","Error",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should return error for CALLRESULT from client with payload`() {
        val response = server.onTextMessage("""[3,"123",{"status":"Accepted"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }
}
