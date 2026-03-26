package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OcppMessageTest {

    @Test
    fun `should parse CALL message correctly`() {
        val json = """[2,"19223201","BootNotification",{"chargePointVendor":"VendorX","chargePointModel":"SingleSocketCharger"}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.Call
        
        assertEquals(OcppMessageType.CALL, message.type)
        assertEquals("19223201", message.messageId)
        assertEquals("BootNotification", message.action)
        assertNotNull(message.payload)
        assertEquals("VendorX", message.payload?.get("chargePointVendor"))
        assertEquals("SingleSocketCharger", message.payload?.get("chargePointModel"))
    }

    @Test
    fun `should parse CALLRESULT message correctly`() {
        val json = """[3,"19223201",{"status":"Accepted","currentTime":"2013-02-01T20:53:32.486Z","interval":300}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallResult
        
        assertEquals(OcppMessageType.CALLRESULT, message.type)
        assertEquals("19223201", message.messageId)
        assertEquals("Accepted", message.payload?.get("status"))
        assertEquals(300, message.payload?.get("interval"))
    }

    @Test
    fun `should parse CALLERROR message correctly`() {
        val json = """[4,"19223201","NotImplemented","Action unknown",{}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallError
        
        assertEquals(OcppMessageType.CALLERROR, message.type)
        assertEquals("19223201", message.messageId)
        assertEquals(OcppErrorCode.NOT_IMPLEMENTED, message.errorCode)
        assertEquals("Action unknown", message.errorDescription)
        assertNotNull(message.errorDetails)
    }

    @Test
    fun `should create CALL message correctly`() {
        val message = OcppMessage.Call(
            messageId = "123",
            action = "Heartbeat",
            payload = mapOf<String, Any>()
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[2,"))
        assertTrue(json.contains("\"123\""))
        assertTrue(json.contains("\"Heartbeat\""))
    }

    @Test
    fun `should create CALLRESULT message correctly`() {
        val message = OcppMessage.CallResult(
            messageId = "123",
            payload = mapOf("currentTime" to "2024-01-01T00:00:00Z")
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[3,"))
        assertTrue(json.contains("\"123\""))
        assertTrue(json.contains("2024-01-01T00:00:00Z"))
    }

    @Test
    fun `should create CALLERROR message correctly`() {
        val message = OcppMessage.CallError(
            messageId = "123",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Invalid payload",
            errorDetails = mapOf<String, Any>()
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[4,"))
        assertTrue(json.contains("\"123\""))
        assertTrue(json.contains("ProtocolError"))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid json",
        "[1,\"id\",\"action\"]",
        "[2,\"id\"]",
        "[5,\"id\",\"action\",{}]"
    ])
    fun `should throw exception for invalid messages`(invalidJson: String) {
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(invalidJson)
        }
    }

    @Test
    fun `should reject messageId longer than 36 characters`() {
        val longId = "a".repeat(37)
        val json = """[2,"$longId","BootNotification",{}]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should accept messageId exactly 36 characters`() {
        val exactId = "a".repeat(36)
        val json = """[2,"$exactId","BootNotification",{}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.Call
        
        assertEquals(exactId, message.messageId)
    }

    @Test
    fun `should parse message with exactly 2 elements and fail`() {
        val json = """[2,"123"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should parse CALL with null payload`() {
        val json = """[2,"123","BootNotification",null]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.Call
        
        assertNull(message.payload)
    }

    @Test
    fun `should parse CALLRESULT with null payload`() {
        val json = """[3,"123",null]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallResult
        
        assertNull(message.payload)
    }

    @Test
    fun `should parse CALLERROR with null errorDetails`() {
        val json = """[4,"123","ProtocolError","Invalid message",null]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallError
        
        assertNull(message.errorDetails)
    }

    @Test
    fun `should create CALL with null payload`() {
        val message = OcppMessage.Call(
            messageId = "123",
            action = "Heartbeat",
            payload = null
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[2,"))
        assertTrue(json.contains("\"Heartbeat\""))
    }

    @Test
    fun `should create CALLRESULT with null payload`() {
        val message = OcppMessage.CallResult(
            messageId = "123",
            payload = null
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[3,"))
    }

    @Test
    fun `should create CALLERROR with null errorDetails`() {
        val message = OcppMessage.CallError(
            messageId = "123",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Invalid message",
            errorDetails = null
        )
        
        val json = message.toJson()
        assertTrue(json.startsWith("[4,"))
        assertTrue(json.contains("ProtocolError"))
    }

    @Test
    fun `should parse CALLERROR with errorDetails object`() {
        val json = """[4,"123","ProtocolError","Invalid message",{"key":"value"}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallError
        
        assertNotNull(message.errorDetails)
        assertEquals("value", message.errorDetails?.get("key"))
    }

    @Test
    fun `should reject CALL with 3 elements`() {
        val json = """[2,"123","Action"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALL with 5 elements`() {
        val json = """[2,"123","Action",{},{}]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALLRESULT with 2 elements`() {
        val json = """[3,"123"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALLRESULT with 4 elements`() {
        val json = """[3,"123",{},{}]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALLERROR with 4 elements`() {
        val json = """[4,"123","ProtocolError","Invalid message"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALLERROR with 6 elements`() {
        val json = """[4,"123","ProtocolError","Invalid message",{},{}]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }
}
