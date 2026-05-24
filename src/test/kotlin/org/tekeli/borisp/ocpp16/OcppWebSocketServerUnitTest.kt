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

    @Test
    fun `should return Accepted for valid StopTransaction`() {
        val response = server.onTextMessage("""[2,"stop-1","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("stop-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with null payload`() {
        val response = server.onTextMessage("""[2,"stop-2","StopTransaction",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with missing transactionId`() {
        val response = server.onTextMessage("""[2,"stop-3","StopTransaction",{"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("transactionId is required"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with missing meterStop`() {
        val response = server.onTextMessage("""[2,"stop-4","StopTransaction",{"transactionId":1,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterStop is required"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with missing timestamp`() {
        val response = server.onTextMessage("""[2,"stop-5","StopTransaction",{"transactionId":1,"meterStop":5000,"reason":"Local"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with empty timestamp`() {
        val response = server.onTextMessage("""[2,"stop-6","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"","reason":"Local"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required"))
    }

    @Test
    fun `should return Accepted for StopTransaction with missing reason (optional per spec)`() {
        val response = server.onTextMessage("""[2,"stop-7","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return Accepted for StopTransaction with empty reason (optional per spec)`() {
        val response = server.onTextMessage("""[2,"stop-8","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":""}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with invalid reason`() {
        val response = server.onTextMessage("""[2,"stop-9","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"InvalidReason"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid reason"))
    }

    @Test
    fun `should return Accepted for StopTransaction with optional idTag`() {
        val response = server.onTextMessage("""[2,"stop-10","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"TAG123"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for StopTransaction with idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage("""[2,"stop-11","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$longIdTag"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("idTag must not exceed 20 characters"))
    }

    @Test
    fun `should return Accepted for StopTransaction with empty idTag`() {
        val response = server.onTextMessage("""[2,"stop-13","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":""}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return Accepted for StopTransaction with idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage("""[2,"stop-12","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$maxIdTag"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return empty CallResult for valid StatusNotification`() {
        val response = server.onTextMessage("""[2,"sn-1","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("sn-1"))
        assertTrue(response.contains("{}"), "Response payload must be empty object, not null")
    }

    @Test
    fun `should return FormationViolation for StatusNotification with null payload`() {
        val response = server.onTextMessage("""[2,"sn-2","StatusNotification",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with missing connectorId`() {
        val response = server.onTextMessage("""[2,"sn-3","StatusNotification",{"errorCode":"NoError","status":"Available"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with missing errorCode`() {
        val response = server.onTextMessage("""[2,"sn-4","StatusNotification",{"connectorId":1,"status":"Available"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("errorCode is required"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with empty errorCode`() {
        val response = server.onTextMessage("""[2,"sn-5","StatusNotification",{"connectorId":1,"errorCode":"","status":"Available"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("errorCode is required"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with invalid errorCode`() {
        val response = server.onTextMessage("""[2,"sn-6","StatusNotification",{"connectorId":1,"errorCode":"InvalidError","status":"Available"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid errorCode"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with missing status`() {
        val response = server.onTextMessage("""[2,"sn-7","StatusNotification",{"connectorId":1,"errorCode":"NoError"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with empty status`() {
        val response = server.onTextMessage("""[2,"sn-8","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with invalid status`() {
        val response = server.onTextMessage("""[2,"sn-9","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"InvalidStatus"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }

    @Test
    fun `should return empty CallResult for StatusNotification with optional info field`() {
        val response = server.onTextMessage("""[2,"sn-10","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"Connector OK"}]""")

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for StatusNotification with info exceeding 50 characters`() {
        val longInfo = "A".repeat(51)
        val response = server.onTextMessage("""[2,"sn-11","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"$longInfo"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("info must not exceed 50 characters"))
    }

    @Test
    fun `should return empty CallResult for StatusNotification with info exactly 50 characters`() {
        val maxInfo = "A".repeat(50)
        val response = server.onTextMessage("""[2,"sn-12","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available","info":"$maxInfo"}]""")

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return Accepted for valid DataTransfer`() {
        val response = server.onTextMessage("""[2,"dt-1","DataTransfer",{"vendorId":"VendorX"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("dt-1"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return FormationViolation for DataTransfer with null payload`() {
        val response = server.onTextMessage("""[2,"dt-2","DataTransfer",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for DataTransfer with missing vendorId`() {
        val response = server.onTextMessage("""[2,"dt-3","DataTransfer",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId is required"))
    }

    @Test
    fun `should return FormationViolation for DataTransfer with empty vendorId`() {
        val response = server.onTextMessage("""[2,"dt-4","DataTransfer",{"vendorId":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId is required"))
    }

    @Test
    fun `should return FormationViolation for DataTransfer with vendorId exceeding 255 characters`() {
        val longVendorId = "A".repeat(256)
        val response = server.onTextMessage("""[2,"dt-5","DataTransfer",{"vendorId":"$longVendorId"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("vendorId must not exceed 255 characters"))
    }

    @Test
    fun `should return Accepted for DataTransfer with vendorId exactly 255 characters`() {
        val maxVendorId = "A".repeat(255)
        val response = server.onTextMessage("""[2,"dt-6","DataTransfer",{"vendorId":"$maxVendorId"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return Accepted for DataTransfer with optional messageId and data`() {
        val response = server.onTextMessage("""[2,"dt-7","DataTransfer",{"vendorId":"VendorX","messageId":"diagStart","data":"dGVzdA=="}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return Accepted for DataTransfer with empty messageId`() {
        val response = server.onTextMessage("""[2,"dt-8","DataTransfer",{"vendorId":"VendorX","messageId":""}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `should return empty CallResult for valid FirmwareStatusNotification`() {
        val response = server.onTextMessage("""[2,"fs-1","FirmwareStatusNotification",{"status":"Downloaded"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("fs-1"))
    }

    @Test
    fun `should return FormationViolation for FirmwareStatusNotification with null payload`() {
        val response = server.onTextMessage("""[2,"fs-2","FirmwareStatusNotification",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for FirmwareStatusNotification with missing status`() {
        val response = server.onTextMessage("""[2,"fs-3","FirmwareStatusNotification",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for FirmwareStatusNotification with empty status`() {
        val response = server.onTextMessage("""[2,"fs-5","FirmwareStatusNotification",{"status":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for FirmwareStatusNotification with invalid status`() {
        val response = server.onTextMessage("""[2,"fs-4","FirmwareStatusNotification",{"status":"InvalidStatus"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }

    @Test
    fun `should return empty CallResult for valid DiagnosticsStatusNotification`() {
        val response = server.onTextMessage("""[2,"ds-1","DiagnosticsStatusNotification",{"status":"Uploaded"}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("ds-1"))
    }

    @Test
    fun `should return FormationViolation for DiagnosticsStatusNotification with null payload`() {
        val response = server.onTextMessage("""[2,"ds-2","DiagnosticsStatusNotification",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for DiagnosticsStatusNotification with missing status`() {
        val response = server.onTextMessage("""[2,"ds-3","DiagnosticsStatusNotification",{}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for DiagnosticsStatusNotification with empty status`() {
        val response = server.onTextMessage("""[2,"ds-5","DiagnosticsStatusNotification",{"status":""}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("status is required"))
    }

    @Test
    fun `should return FormationViolation for DiagnosticsStatusNotification with invalid status`() {
        val response = server.onTextMessage("""[2,"ds-4","DiagnosticsStatusNotification",{"status":"InvalidStatus"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Invalid status"))
    }

    @Test
    fun `should return empty CallResult for valid MeterValues`() {
        val response = server.onTextMessage("""[2,"mv-1","MeterValues",{"connectorId":1,"transactionId":123,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000.5","measurand":"Energy.Active.Import.Register"}]}]}]""")

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("mv-1"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with null payload`() {
        val response = server.onTextMessage("""[2,"mv-2","MeterValues",null]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with missing connectorId`() {
        val response = server.onTextMessage("""[2,"mv-3","MeterValues",{"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with negative connectorId`() {
        val response = server.onTextMessage("""[2,"mv-4","MeterValues",{"connectorId":-1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId must be >= 0"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with missing meterValue`() {
        val response = server.onTextMessage("""[2,"mv-5","MeterValues",{"connectorId":1}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue is required"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with empty meterValue array`() {
        val response = server.onTextMessage("""[2,"mv-6","MeterValues",{"connectorId":1,"meterValue":[]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue must contain at least 1 element"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with missing timestamp in meterValue`() {
        val response = server.onTextMessage("""[2,"mv-7","MeterValues",{"connectorId":1,"meterValue":[{"sampledValue":[{"value":"5000"}]}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required in meterValue"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with missing sampledValue`() {
        val response = server.onTextMessage("""[2,"mv-8","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z"}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue is required in meterValue"))
    }

    @Test
    fun `should return empty CallResult for MeterValues with main power meter connectorId 0`() {
        val response = server.onTextMessage("""[2,"mv-9","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]""")

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return empty CallResult for MeterValues with multiple sampledValues`() {
        val response = server.onTextMessage("""[2,"mv-10","MeterValues",{"connectorId":1,"transactionId":456,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000","measurand":"Energy.Active.Import.Register"},{"value":"230.5","measurand":"Voltage"},{"value":"16.2","measurand":"Current.Import"}]}]}]""")

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return empty CallResult for MeterValues with optional transactionId omitted`() {
        val response = server.onTextMessage("""[2,"mv-11","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]""")

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with string meterValue instead of array`() {
        val response = server.onTextMessage("""[2,"mv-12","MeterValues",{"connectorId":1,"meterValue":"not an array"}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue must be an array"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with string sampledValue instead of array`() {
        val response = server.onTextMessage("""[2,"mv-13","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":"not an array"}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue must be an array"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with missing value in sampledValue`() {
        val response = server.onTextMessage("""[2,"mv-15","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"measurand":"Energy"}]}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("value is required in sampledValue"))
    }

    @Test
    fun `should return FormationViolation for MeterValues with empty sampledValue array`() {
        val response = server.onTextMessage("""[2,"mv-16","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[]}]}]""")

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue must contain at least 1 element"))
    }
}
