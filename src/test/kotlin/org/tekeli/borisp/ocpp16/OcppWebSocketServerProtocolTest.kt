package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerProtocolTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return NotImplemented for unknown action`() {
        val response = server.onTextMessage("""[2,"123","UnknownAction",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("'UnknownAction' is not implemented"))
    }

    @Test
    fun `should return ProtocolError for CALLRESULT from client`() {
        val response = server.onTextMessage("""[3,"123",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLRESULT not expected"))
    }

    @Test
    fun `should return ProtocolError for CALLRESULT from client with payload`() {
        val response = server.onTextMessage("""[3,"123",{"status":"Accepted"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should return ProtocolError for CALLERROR from client`() {
        val response = server.onTextMessage("""[4,"123","GenericError","Error",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLERROR not expected from ChargePoint"))
    }

    @Test
    fun `should return ProtocolError for CALLERROR from client with valid error code`() {
        val response = server.onTextMessage("""[4,"123","NotImplemented","Error",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should handle invalid JSON with ProtocolError`() {
        val response = server.onTextMessage("not valid json")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("Failed to parse OCPP message"))
    }

    @Test
    fun `should handle empty string message`() {
        val response = server.onTextMessage("")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("Failed to parse"))
    }

    @Test
    fun `should handle whitespace only message`() {
        val response = server.onTextMessage("   ")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should use valid UUID messageId for parse errors`() {
        val response = server.onTextMessage("not valid json")
        val messageIdRegex = Regex("\\[4,\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",")
        assertTrue(messageIdRegex.containsMatchIn(response), "messageId must be valid UUID format")
    }

    @Test
    fun `should generate unique messageId for each parse error`() {
        val response1 = server.onTextMessage("not valid json")
        val response2 = server.onTextMessage("also invalid")
        assertNotEquals(response1, response2, "Each parse error should have unique messageId")
    }

    @Test
    fun `should return valid CALLERROR structure for parse errors`() {
        val response = server.onTextMessage("invalid")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.endsWith("]"))
    }

    @Test
    fun `should preserve original messageId for NotImplemented errors`() {
        val response = server.onTextMessage("""[2,"custom-id-123","TestAction",{}]""")
        assertTrue(response.contains("custom-id-123"))
        assertTrue(response.contains("NotImplemented"))
    }

    @Test
    fun `should preserve original messageId for FormationViolation errors`() {
        val response = server.onTextMessage("""[2,"my-unique-id","BootNotification",null]""")
        assertTrue(response.contains("my-unique-id"))
        assertTrue(response.contains("FormationViolation"))
    }
}
