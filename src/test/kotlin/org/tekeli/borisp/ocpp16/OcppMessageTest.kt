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

    @Test
    fun `should reject message with invalid type ID 1`() {
        val json = """[1,"123"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject message with invalid type ID 5`() {
        val json = """[5,"123"]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject CALLERROR with invalid error code`() {
        val json = """[4,"123","InvalidErrorCode","Invalid message",{}]"""
        
        assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
    }

    @Test
    fun `should reject message with only 1 element`() {
        val json = """[2]"""
        
        val exception = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
        assertTrue(exception.message!!.contains("at least 2"))
    }

    @Test
    fun `should throw exception with non-null message for invalid json`() {
        val json = "not valid json at all {"
        
        val exception = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should handle CALL with emptyMap payload in toJson`() {
        val message = OcppMessage.Call(
            messageId = "123",
            action = "Heartbeat",
            payload = emptyMap<String, Any>()
        )
        
        val json = message.toJson()
        assertTrue(json.contains("{}"))
    }

    @Test
    fun `should handle CALLRESULT with emptyMap payload in toJson`() {
        val message = OcppMessage.CallResult(
            messageId = "123",
            payload = emptyMap<String, Any>()
        )
        
        val json = message.toJson()
        assertTrue(json.contains("{}"))
    }

    @Test
    fun `should handle CALLERROR with emptyMap errorDetails in toJson`() {
        val message = OcppMessage.CallError(
            messageId = "123",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Test error",
            errorDetails = emptyMap<String, Any>()
        )
        
        val json = message.toJson()
        assertTrue(json.contains("{}"))
    }

    @Test
    fun `should parse CALL with empty object payload`() {
        val json = """[2,"123","Heartbeat",{}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.Call
        
        assertNotNull(message.payload)
        assertTrue(message.payload!!.isEmpty())
    }

    @Test
    fun `should parse CALLRESULT with empty object payload`() {
        val json = """[3,"123",{}]"""
        
        val message = OcppMessage.parse(json) as OcppMessage.CallResult
        
        assertNotNull(message.payload)
        assertTrue(message.payload!!.isEmpty())
    }

     @Test
    fun `should parse CALLERROR with empty object errorDetails`() {
        val json = """[4,"123","ProtocolError","Test",{}]"""

        val message = OcppMessage.parse(json) as OcppMessage.CallError

        assertNotNull(message.errorDetails)
        assertTrue(message.errorDetails!!.isEmpty())
    }

    // Tests to kill SURVIVED mutants

    @Test
    fun `should throw OcppParseException for invalid message type`() {
        val json = """[99,"123","Action",{}]"""

        val exception = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
        assertTrue(exception.message!!.contains("Invalid message type"))
    }

    @Test
    fun `should throw OcppParseException for invalid error code`() {
        val json = """[4,"123","InvalidErrorCode","Description",{}]"""

        val exception = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse(json)
        }
        assertTrue(exception.message!!.contains("Invalid error code"))
    }

    @Test
    fun `should serialize Call with null payload to empty object`() {
        val call = OcppMessage.Call(messageId = "123", action = "Test", payload = null)
        val json = call.toJson()

        assertTrue(json.contains("[2,\"123\",\"Test\",{}]"))
    }

    @Test
    fun `should serialize CallResult with null payload to empty object`() {
        val callResult = OcppMessage.CallResult(messageId = "123", payload = null)
        val json = callResult.toJson()

        assertTrue(json.contains("[3,\"123\",{}]"))
    }

    @Test
    fun `should serialize CallError with null errorDetails to empty object`() {
        val callError = OcppMessage.CallError(
            messageId = "123",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Test",
            errorDetails = null
        )
        val json = callError.toJson()

        assertTrue(json.contains("[4,\"123\",\"ProtocolError\",\"Test\",{}]"))
    }

    @Test
    fun `should serialize Call with non-null payload preserving all fields`() {
        val call = OcppMessage.Call(
            messageId = "123",
            action = "BootNotification",
            payload = mapOf("vendor" to "Tesla", "model" to "Model3")
        )
        val json = call.toJson()

        assertTrue(json.contains("\"vendor\""))
        assertTrue(json.contains("\"Tesla\""))
        assertTrue(json.contains("\"model\""))
        assertTrue(json.contains("\"Model3\""))
    }

    @Test
    fun `should serialize CallResult with non-null payload preserving all fields`() {
        val callResult = OcppMessage.CallResult(
            messageId = "123",
            payload = mapOf("status" to "Accepted", "currentTime" to "2024-01-01T00:00:00Z")
        )
        val json = callResult.toJson()

        assertTrue(json.contains("\"status\""))
        assertTrue(json.contains("\"Accepted\""))
        assertTrue(json.contains("\"currentTime\""))
    }

    @Test
    fun `should serialize CallError with non-null errorDetails preserving all fields`() {
        val callError = OcppMessage.CallError(
            messageId = "123",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Test",
            errorDetails = mapOf("code" to 500, "message" to "Internal Error")
        )
        val json = callError.toJson()

        assertTrue(json.contains("\"code\""))
        assertTrue(json.contains("500"))
        assertTrue(json.contains("\"message\""))
        assertTrue(json.contains("\"Internal Error\""))
    }

    @Test
    fun `should parse CALL with null payload and preserve null`() {
        val json = """[2,"123","Heartbeat",null]"""
        val message = OcppMessage.parse(json) as OcppMessage.Call

        assertNull(message.payload)
    }

    @Test
    fun `should parse CALLRESULT with null payload and preserve null`() {
        val json = """[3,"123",null]"""
        val message = OcppMessage.parse(json) as OcppMessage.CallResult

        assertNull(message.payload)
    }
}
