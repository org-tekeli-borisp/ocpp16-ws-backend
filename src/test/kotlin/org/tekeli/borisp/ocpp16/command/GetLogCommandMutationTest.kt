package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import java.util.concurrent.CompletableFuture

class GetLogCommandMutationTest {

    private val gateway = TestGetLogGateway()
    private fun makeCallResult() = OcppMessage.CallResult("id", mapOf())
    private fun makeCallError() = OcppMessage.CallError("id", OcppErrorCode.PROTOCOL_ERROR, "err", null)

    // -- name property tests (kills getName mutators) --
    @Test
    fun `name property equals get-log`() {
        val cmd = GetLogCommand(gateway)
        assertEquals("get-log", cmd.name)
    }

    // -- validate() tests --

    @Test
    fun `validate returns null for valid DiagnosticsLog payload`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "DiagnosticsLog",
            "requestId" to 42,
            "log" to mapOf("remoteLocation" to "https://example.com/diag")
        ))
        assertNull(response)
    }

    @Test
    fun `validate returns null for valid SecurityLog payload`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 99,
            "log" to mapOf("remoteLocation" to "https://example.com/sec")
        ))
        assertNull(response)
    }

    @Test
    fun `validate rejects missing logType`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("logType"))
        assertTrue(entity["error"].toString().contains("DiagnosticsLog"))
        assertTrue(entity["error"].toString().contains("SecurityLog"))
    }

    @Test
    fun `validate rejects invalid logType`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "InvalidLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("logType"))
    }

    @Test
    fun `validate rejects missing requestId`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestId"))
    }

    @Test
    fun `validate rejects requestId as String instead of Number`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to "not-a-number",
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestId"))
    }

    @Test
    fun `validate accepts requestId as Long`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 999L,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNull(response)
    }

    @Test
    fun `validate accepts requestId as Int`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 42,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNull(response)
    }

    @Test
    fun `validate accepts requestId as Double`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 42.0,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNull(response)
    }

    @Test
    fun `validate rejects missing log`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("log"))
    }

    @Test
    fun `validate rejects log as non-Map type`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to "not-a-map"
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("log"))
    }

    @Test
    fun `validate rejects missing remoteLocation inside log`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf<String, Any>()
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("remoteLocation"))
    }

    @Test
    fun `validate rejects empty remoteLocation`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("remoteLocation"))
        assertTrue(entity["error"].toString().contains("512"))
    }

    @Test
    fun `validate accepts remoteLocation exactly 512 chars`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "A".repeat(512))
        ))
        assertNull(response)
    }

    @Test
    fun `validate rejects remoteLocation exceeding 512 chars`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "A".repeat(513))
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("remoteLocation"))
        assertTrue(entity["error"].toString().contains("512"))
    }

    @Test
    fun `validate with error returns Response with entity containing error key`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "BadType", "requestId" to 1, "log" to mapOf("remoteLocation" to "url")))
        assertNotNull(response)
        val entity = PayloadValidators.safeMap(response!!.entity)
        assertNotNull(entity["error"])
        assertEquals(String::class.java, (entity["error"] as Any)::class.java)
    }

    // -- execute() success path tests --

    @Test
    fun `execute returns 202 with status sent and command get-log`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
    }

    @Test
    fun `execute success with DiagnosticsLog`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "DiagnosticsLog",
            "requestId" to 456,
            "log" to mapOf("remoteLocation" to "https://example.com/diag")
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
        assertEquals("DiagnosticsLog", gateway.capturedLogType)
    }

    @Test
    fun `execute success with SecurityLog`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 789,
            "log" to mapOf("remoteLocation" to "https://example.com/sec")
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
        assertEquals("SecurityLog", gateway.capturedLogType)
    }

    @Test
    fun `execute forwards correct requestId to gateway`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 42,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertEquals(42, gateway.capturedRequestId)
    }

    @Test
    fun `execute forwards requestId as Long converted to Int`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 777L,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertEquals(777, gateway.capturedRequestId)
    }

    @Test
    fun `execute forwards correct chargePointId to gateway`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-999", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertEquals("CP-999", gateway.capturedChargePointId)
    }

    @Test
    fun `execute forwards correct log map to gateway`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val logPayload = mapOf("remoteLocation" to "https://myserver.com/upload", "logType" to "SecurityLog")
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to logPayload
        ))
        assertEquals("https://myserver.com/upload", gateway.capturedLog?.get("remoteLocation"))
    }

    @Test
    fun `execute with retries and retryInterval forwards values`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 10,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retries" to 5,
            "retryInterval" to 30
        ))
        assertEquals(5, gateway.capturedRetries)
        assertEquals(30, gateway.capturedRetryInterval)
    }

    @Test
    fun `execute without retries and retryInterval sends null`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 10,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNull(gateway.capturedRetries)
        assertNull(gateway.capturedRetryInterval)
    }

    @Test
    fun `execute with retries as Long converts to Int`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 10,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retries" to 3L,
            "retryInterval" to 60L
        ))
        assertEquals(3, gateway.capturedRetries)
        assertEquals(60, gateway.capturedRetryInterval)
    }

    @Test
    fun `execute sends sendGetLog to gateway`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertTrue(gateway.sendGetLogCalled)
    }

    // -- execute() failure path tests --

    @Test
    fun `execute returns 502 when ChargePoint rejects`() {
        gateway.lastResponse = makeCallError()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(502, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `execute failure path still calls gateway sendGetLog`() {
        gateway.lastResponse = makeCallError()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "DiagnosticsLog",
            "requestId" to 55,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertTrue(gateway.sendGetLogCalled)
        assertEquals("DiagnosticsLog", gateway.capturedLogType)
    }

    @Test
    fun `execute failure with retries still calls gateway`() {
        gateway.lastResponse = makeCallError()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retries" to 2,
            "retryInterval" to 10
        ))
        assertTrue(gateway.sendGetLogCalled)
        assertEquals(2, gateway.capturedRetries)
        assertEquals(10, gateway.capturedRetryInterval)
    }

    @Test
    fun `execute failure entity contains error key with correct message`() {
        gateway.lastResponse = makeCallError()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
        assertNotNull(entity["error"])
    }

    // -- boundary and conditional tests --

    @Test
    fun `validate passes with remoteLocation of 1 char`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "x")
        ))
        assertNull(response)
    }

    @Test
    fun `validate passes with remoteLocation of 511 chars`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "B".repeat(511))
        ))
        assertNull(response)
    }

    @Test
    fun `validate fails with remoteLocation of 513 chars`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "DiagnosticsLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "C".repeat(513))
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `execute with requestId 0 works correctly`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 0,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertEquals(202, response.status)
        assertEquals(0, gateway.capturedRequestId)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
    }

    @Test
    fun `execute with retries 0 passes through`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retries" to 0
        ))
        assertEquals(0, gateway.capturedRetries)
    }

    // == MUTATION KILL TESTS for surviving mutants ==

    // Kill L37 validateNestedLog EQUAL_IF: remoteLocation as? String replaced with true
    // When remoteLocation is not a String (e.g., an Int), the original safely returns error.
    // The mutant skips the null-safe cast, causing ClassCastException downstream.
    @Test
    fun `validateNestedLog rejects remoteLocation as non-String type - kills as? String EQUAL_IF`() {
        val cmd = GetLogCommand(gateway)
        val error = cmd.validateNestedLog(mapOf(
            "log" to mapOf("remoteLocation" to 42)
        ))
        assertNotNull(error)
        assertEquals("log.remoteLocation is required", error)
    }

    // Kill L28 validateTopLevel EQUAL_IF: logType as? String replaced with true
    // When logType is missing (null), original returns error.
    // Mutant: logType becomes null, then `null !in validLogTypes` = true, returns error.
    // To distinguish: when logType is a non-String type (like Integer), original casts to null,
    // mutant passes it through and crashes on `!in` comparison or returns wrong error.
    @Test
    fun `validateTopLevel rejects logType as non-String type - kills as? String EQUAL_IF`() {
        val cmd = GetLogCommand(gateway)
        val error = cmd.validateTopLevel(mapOf(
            "logType" to 123,
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        assertNotNull(error)
        assertEquals("logType must be one of: DiagnosticsLog, SecurityLog", error)
    }

    // Kill L29 validateTopLevel EQUAL_IF: logType !in validLogTypes replaced with true
    // When logType is valid but requestId is missing, original returns requestId error.
    // Mutant returns logType error (because condition always true).
    @Test
    fun `validateTopLevel with valid logType but missing requestId returns requestId error - kills !in EQUAL_IF`() {
        val cmd = GetLogCommand(gateway)
        val error = cmd.validateTopLevel(mapOf(
            "logType" to "DiagnosticsLog",
            "log" to mapOf("remoteLocation" to "https://example.com")
        ))
        // Original: logType is valid, so skips to requestId check -> returns requestId error
        // Mutant: !in replaced with true -> returns logType error
        assertNotNull(error)
        assertTrue(
            error!!.contains("requestId"),
            "Should return requestId error, not logType error. Got: $error"
        )
    }

    // Kill L47 execute EQUAL_IF: retries as? Number replaced with true
    // When retries is present as a String, original safely gets null.
    // Mutant: the safe cast becomes unconditional, tries to cast String to Number -> fails
    // We test by passing retries as String value - original ignores it (null), mutant crashes
    @Test
    fun `execute handles retries as non-Number gracefully - kills retries EQUAL_IF`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        // retries as String should be ignored (treated as null)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retries" to "not-a-number"
        ))
        assertEquals(202, response.status)
        assertNull(gateway.capturedRetries)
    }

    // Kill L48 execute EQUAL_IF: retryInterval as? Number replaced with true
    // Same pattern: retryInterval as String should be ignored
    @Test
    fun `execute handles retryInterval as non-Number gracefully - kills retryInterval EQUAL_IF`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "https://example.com"),
            "retryInterval" to "not-a-number"
        ))
        assertEquals(202, response.status)
        assertNull(gateway.capturedRetryInterval)
    }

    // Test double that captures all parameters sent to sendGetLog
    private class TestGetLogGateway : ChargePointGateway {
        var lastResponse: OcppMessage? = OcppMessage.CallResult("id", mapOf())
        var sendGetLogCalled = false
        var capturedChargePointId: String? = null
        var capturedLogType: String? = null
        var capturedRequestId: Int? = null
        var capturedLog: Map<String, Any>? = null
        var capturedRetries: Int? = null
        var capturedRetryInterval: Int? = null

        override fun sendGetLog(
            chargePointId: String,
            logType: String,
            requestId: Int,
            log: Map<String, Any>,
            retries: Int?,
            retryInterval: Int?
        ): CompletableFuture<OcppMessage> {
            sendGetLogCalled = true
            capturedChargePointId = chargePointId
            capturedLogType = logType
            capturedRequestId = requestId
            capturedLog = log
            capturedRetries = retries
            capturedRetryInterval = retryInterval
            return CompletableFuture.completedFuture(lastResponse!!)
        }

        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetConfiguration(chargePointId: String, keys: List<String>?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?, startTime: String?, stopTime: String?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = TODO()
    }
}
