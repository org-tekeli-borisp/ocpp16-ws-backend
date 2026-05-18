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
        // Specific check to kill RemoveConditional_EQUAL_IF mutation at line 70
        assertTrue(response.contains("'UnknownAction' is not implemented"))
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
    fun `should handle CallError branch specifically`() {
        // This aims to kill the SURVIVED mutant at line 45
        val response = server.onTextMessage("""[4,"test-id","GenericError","SomeError",{}]""")
        
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLERROR not expected from ChargePoint"))
    }

    @Test
    fun `should handle Call branch specifically`() {
        // This aims to kill the TIME_OUT mutants at line 36
        val response = server.onTextMessage("""[2,"test-id","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]""")
        
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should handle BootNotification with empty vendor string`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle FormationViolation with default error message`() {
        // Create a call with null payload to trigger FormationViolationException
        val response = server.onTextMessage("""[2,"test-id","BootNotification",null]""")
        
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        // Specific check to kill RemoveConditional_EQUAL_ELSE mutation at line 82
        assertTrue(response.contains("Payload validation failed") || response.contains("Payload is null"))
    }

    @Test
    fun `should handle parse error with default error message`() {
        val response = server.onTextMessage("not valid json")
        
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        // Specific check to kill NegateConditionals mutation at line 55 (index 111)
        assertTrue(response.contains("Failed to parse OCPP message"))
    }

    @Test
    fun `should handle parse error with null exception message`() {
        // Test the ?: default value path in catch block
        val response = server.onTextMessage("invalid")
        
        assertTrue(response.startsWith("[4,"))
        // Must contain the error message to kill mutation
        assertTrue(response.contains("Failed to parse"))
    }

    @Test
    fun `should handle formation violation with null exception message`() {
        // Test the ?: default value path in catch block at line 82
        val response = server.onTextMessage("""[2,"test-id","BootNotification",null]""")
        
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        // Must contain specific error message to kill mutation
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should handle callerror with specific error message`() {
        // Kill RemoveConditional_EQUAL_IF mutation at line 45 (index 64)
        val response = server.onTextMessage("""[4,"test-id","NotImplemented","Error",{}]""")
        
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLERROR not expected from ChargePoint"))
    }

    @Test
    fun `should kill all line 82 mutations with specific error messages`() {
        // Test specific error message path to kill RemoveConditional_EQUAL_IF mutations at line 82
        // The mutations are at indexes 65, 78, 90, 100 in the catch block
        val response1 = server.onTextMessage("""[2,"id1","BootNotification",{"chargePointVendor":"","chargePointModel":"M"}]""")
        assertTrue(response1.contains("chargePointVendor is required"))
        assertTrue(response1.contains("FormationViolation"))
        
        val response2 = server.onTextMessage("""[2,"id2","BootNotification",{"chargePointVendor":"V","chargePointModel":""}]""")
        assertTrue(response2.contains("chargePointModel is required"))
        assertTrue(response2.contains("FormationViolation"))
        
        val response3 = server.onTextMessage("""[2,"id3","BootNotification",null]""")
        assertTrue(response3.contains("Payload is null"))
        assertTrue(response3.contains("FormationViolation"))
    }

    @Test
    fun `should kill all line 55 mutations with parse error messages`() {
        // Test multiple parse error paths to kill mutations at line 55
        // The mutations are at indexes 111, 124, 136, 146 in the catch block
        val response1 = server.onTextMessage("invalid json")
        assertTrue(response1.startsWith("[4,"))
        assertTrue(response1.contains("ProtocolError"))
        assertTrue(response1.contains("Failed to parse OCPP message"))
        
        val response2 = server.onTextMessage("")
        assertTrue(response2.startsWith("[4,"))
        assertTrue(response2.contains("ProtocolError"))
        assertTrue(response2.contains("Failed to parse"))
        
        val response3 = server.onTextMessage("   ")
        assertTrue(response3.startsWith("[4,"))
        assertTrue(response3.contains("ProtocolError"))
        assertTrue(response3.contains("Failed to parse"))
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
        assertFalse(response.contains("FormationViolation"), "Valid request should not return FormationViolation")
    }

    @Test
    fun `should handle BootNotification with vendor containing whitespace`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":" ","chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle BootNotification with null vendor field`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":null,"chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle BootNotification with missing vendor field`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointModel":"Model3"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle BootNotification with null model field`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":null}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should handle BootNotification with missing model field`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Tesla"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
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
        // messageId must be a valid UUID format (8-4-4-4-12 hex digits)
        val messageIdRegex = Regex("\\[4,\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",")
        assertTrue(messageIdRegex.containsMatchIn(response), "messageId must be valid UUID format")
    }

    @Test
    fun `should generate unique message id for each parse error`() {
        val response1 = server.onTextMessage("not valid json")
        val response2 = server.onTextMessage("also invalid")
        assertNotEquals(response1, response2, "Each parse error should have unique messageId")
    }

    @Test
    fun `should handle empty string message`() {
        val response = server.onTextMessage("")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should handle whitespace only message`() {
        val response = server.onTextMessage("   ")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should use default error message when exception message is null`() {
        val response = server.onTextMessage("not valid json")
        assertTrue(response.contains("ProtocolError"))
        // Either contains the parse error message or the default message
        assertTrue(
            response.contains("Failed to parse") || response.contains("Failed to parse OCPP message"),
            "Should contain error description"
        )
    }

    @Test
    fun `should use default error message when exception message is blank`() {
        val response = server.onTextMessage("   ")
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `should return valid CALLERROR structure for parse errors`() {
        val response = server.onTextMessage("invalid")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.endsWith("]"))
    }

    @Test
    fun `should handle FormationViolation with non-null message`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",null]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("\"123\""), "Should use original messageId")
    }

    @Test
    fun `should handle FormationViolation with custom error message`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",null]""")
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should handle FormationViolation preserving original messageId`() {
        val response = server.onTextMessage("""[2,"my-unique-id","BootNotification",null]""")
        assertTrue(response.contains("my-unique-id"), "FormationViolation should preserve original messageId")
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

    @Test
    fun `should return NotImplemented error with exact message format`() {
        val response = server.onTextMessage("""[2,"456","UnknownAction",{}]""")
        assertTrue(response.contains("Action 'UnknownAction' is not implemented"))
    }

    @Test
    fun `should preserve original messageId for NotImplemented errors`() {
        val response = server.onTextMessage("""[2,"custom-id-123","TestAction",{}]""")
        assertTrue(response.contains("custom-id-123"))
        assertTrue(response.contains("NotImplemented"))
    }

    @Test
    fun `should handle BootNotification with whitespace-only vendor`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"   ","chargePointModel":"Model"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should handle BootNotification with whitespace-only model`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"   "}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should handle valid BootNotification returns Accepted status`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model"}]""")
        assertTrue(response.contains("Accepted"))
        assertFalse(response.contains("FormationViolation"))
    }

    @Test
    fun `should return FormationViolation for empty payload map`() {
        val response = server.onTextMessage("""[2,"123","BootNotification",{}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should use original messageId when vendor validation fails`() {
        val response = server.onTextMessage("""[2,"vendor-error-id","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")
        assertTrue(response.contains("vendor-error-id"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should use original messageId when model validation fails`() {
        val response = server.onTextMessage("""[2,"model-error-id","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":""}]""")
        assertTrue(response.contains("model-error-id"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should test handler null branch explicitly`() {
        val response = server.onTextMessage("""[2,"test-id","NonExistentAction",{}]""")
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("NonExistentAction"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should test vendor isBlank branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"   ","chargePointModel":"Model"}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should test model isBlank branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"   "}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should test vendor null branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":null,"chargePointModel":"Model"}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should test model null branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":null}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should test vendor empty string branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"","chargePointModel":"Model"}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should test model empty string branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":""}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should test payload null branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",null]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should test missing vendor field branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointModel":"Model"}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointVendor is required"))
    }

    @Test
    fun `should test missing model field branch`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",{"chargePointVendor":"Vendor"}]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("chargePointModel is required"))
    }

    @Test
    fun `should test unknown action handler null branch`() {
        val response = server.onTextMessage("""[2,"my-id","UnknownAction",{}]""")
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("my-id"))
    }

    @Test
    fun `should test callresult protocol error branch`() {
        val response = server.onTextMessage("""[3,"id",{}]""")
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLRESULT not expected"))
    }

    @Test
    fun `should test calerror protocol error branch`() {
        val response = server.onTextMessage("""[4,"id","GenericError","desc",{}]""")
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("CALLERROR not expected"))
    }

    @Test
    fun `should test parse error exception branch`() {
        val response = server.onTextMessage("invalid json")
        assertTrue(response.contains("ProtocolError"))
        assertTrue(response.contains("Failed to parse"))
    }

    @Test
    fun `should test heartbeat success branch`() {
        val response = server.onTextMessage("""[2,"id","Heartbeat",{}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("currentTime"))
        assertFalse(response.contains("FormationViolation"))
        assertFalse(response.contains("NotImplemented"))
    }

    @Test
    fun `should kill line 82 mutation index 100 by testing FormationViolation with default message`() {
        val response = server.onTextMessage("""[2,"id","BootNotification",null]""")
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
        // The mutation replaces the conditional with false, so we need to ensure the default message is NOT used
        // This kills the mutation by testing the non-default path
        assertTrue(response.contains("Payload is null") && !response.contains("Payload validation failed"))
    }

    @Test
    fun `should throw IllegalStateException when activeConnection is accessed without initialization`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            server.activeConnection
        }
        assertEquals("Connection not initialized", exception.message)
    }

    @Test
    fun `should return Accepted for valid Authorize request`() {
        val response = server.onTextMessage("""[2,"auth-1","Authorize",{"idTag":"ABC123"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("auth-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for Authorize with null payload`() {
        val response = server.onTextMessage("""[2,"auth-2","Authorize",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for Authorize with missing idTag`() {
        val response = server.onTextMessage("""[2,"auth-3","Authorize",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for Authorize with empty idTag`() {
        val response = server.onTextMessage("""[2,"auth-4","Authorize",{"idTag":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for Authorize with idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"auth-5","Authorize",{"idTag":"$longIdTag"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should return Accepted for Authorize with max length idTag 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"auth-6","Authorize",{"idTag":"$maxIdTag"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should preserve messageId in Authorize response`() {
        val response = server.onTextMessage("""[2,"custom-msg-id","Authorize",{"idTag":"TAG001"}]""")

        assertTrue(response.contains("custom-msg-id"))
    }

    @Test
    fun `should return Accepted with transactionId for valid StartTransaction`() {
        val response = server.onTextMessage("""[2,"st-1","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("st-1"))
        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("transactionId"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with null payload`() {
        val response = server.onTextMessage("""[2,"st-2","StartTransaction",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with missing connectorId`() {
        val response = server.onTextMessage("""[2,"st-3","StartTransaction",{"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with missing idTag`() {
        val response = server.onTextMessage("""[2,"st-4","StartTransaction",{"connectorId":1,"meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with empty idTag`() {
        val response = server.onTextMessage("""[2,"st-5","StartTransaction",{"connectorId":1,"idTag":"","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag is required"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"st-6","StartTransaction",{"connectorId":1,"idTag":"$longIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should return Accepted for StartTransaction with idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"st-12","StartTransaction",{"connectorId":1,"idTag":"$maxIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with missing meterStart`() {
        val response = server.onTextMessage("""[2,"st-7","StartTransaction",{"connectorId":1,"idTag":"ABC123","timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterStart is required"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with missing timestamp`() {
        val response = server.onTextMessage("""[2,"st-8","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should return FormationViolation for StartTransaction with empty timestamp`() {
        val response = server.onTextMessage("""[2,"st-9","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should handle StartTransaction with optional reservationId`() {
        val response = server.onTextMessage("""[2,"st-10","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z","reservationId":42}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return transactionId greater than 0 for StartTransaction`() {
        val response = server.onTextMessage("""[2,"st-11","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]""")

        assertTrue(response.contains("\"transactionId\":1"))
    }
}
