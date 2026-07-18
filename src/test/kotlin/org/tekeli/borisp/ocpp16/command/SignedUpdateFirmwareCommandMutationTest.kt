package org.tekeli.borisp.ocpp16.command

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.SignedFirmware
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.Instant
import java.util.concurrent.CompletableFuture

class SignedUpdateFirmwareCommandMutationTest {

    private val gateway = TestMutSecurityGateway()
    private val cmd = SignedUpdateFirmwareCommand(gateway)

    private fun makeValidFirmware(
        location: String = "https://example.com/fw.bin",
        retrieveDateTime: String = "2024-01-01T00:00:00Z",
        signingCertificate: String = "cert",
        signature: String = "sig"
    ) = mapOf(
        "location" to location,
        "retrieveDateTime" to retrieveDateTime,
        "signingCertificate" to signingCertificate,
        "signature" to signature
    )

    private fun makeCallResult() = OcppMessage.CallResult("call-id", mapOf())
    private fun makeCallError() = OcppMessage.CallError("call-id", OcppErrorCode.PROTOCOL_ERROR, "err", null)

    // ===== name property =====

    @Test
    fun `name property equals signed-update-firmware`() {
        assertEquals("signed-update-firmware", cmd.name)
    }

    // ===== validate() - requestId edge cases =====

    @Test
    fun `validate - missing requestId returns 400 with error`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("requestId is required", entity["error"])
    }

    @Test
    fun `validate - requestId as String returns 400 bad request`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to "notANumber", "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `validate - requestId as Long is accepted`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to 123456789L, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - requestId as Int is accepted`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to 123, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - requestId as Double Number is accepted`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to 123.0, "firmware" to firmware))

        assertNull(response)
    }

    // ===== validate() - firmware edge cases =====

    @Test
    fun `validate - missing firmware returns 400`() {
        val response = cmd.validate(mapOf("requestId" to 1))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("firmware is required", entity["error"])
    }

    @Test
    fun `validate - firmware as non-Map returns 400`() {
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to "notAMap"))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("firmware is required", entity["error"])
    }

    @Test
    fun `validate - firmware as List returns 400`() {
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to listOf("a", "b")))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("firmware is required", entity["error"])
    }

    // ===== validate() - location edge cases =====

    @Test
    fun `validate - missing location returns 400`() {
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>()
        ))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
    }

    @Test
    fun `validate - empty location returns 400`() {
        val firmware = makeValidFirmware(location = "")
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.location"))
    }

    @Test
    fun `validate - location exactly 512 chars is accepted`() {
        val firmware = makeValidFirmware(location = "A".repeat(512))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - location 513 chars returns 400`() {
        val firmware = makeValidFirmware(location = "A".repeat(513))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.location"))
    }

    // ===== validate() - retrieveDateTime edge cases =====

    @Test
    fun `validate - missing retrieveDateTime returns 400`() {
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin"
        )
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
    }

    @Test
    fun `validate - empty retrieveDateTime returns 400`() {
        val firmware = makeValidFirmware(retrieveDateTime = "")
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.retrieveDateTime"))
    }

    // ===== validate() - signingCertificate edge cases =====

    @Test
    fun `validate - missing signingCertificate returns 400`() {
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z"
        )
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
    }

    @Test
    fun `validate - empty signingCertificate returns 400`() {
        val firmware = makeValidFirmware(signingCertificate = "")
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.signingCertificate"))
    }

    @Test
    fun `validate - signingCertificate exactly 5500 chars is accepted`() {
        val firmware = makeValidFirmware(signingCertificate = "A".repeat(5500))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - signingCertificate 5501 chars returns 400`() {
        val firmware = makeValidFirmware(signingCertificate = "A".repeat(5501))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.signingCertificate"))
    }

    // ===== validate() - signature edge cases =====

    @Test
    fun `validate - missing signature returns 400`() {
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert"
        )
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
    }

    @Test
    fun `validate - empty signature returns 400`() {
        val firmware = makeValidFirmware(signature = "")
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.signature"))
    }

    @Test
    fun `validate - signature exactly 800 chars is accepted`() {
        val firmware = makeValidFirmware(signature = "A".repeat(800))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - signature 801 chars returns 400`() {
        val firmware = makeValidFirmware(signature = "A".repeat(801))
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware.signature"))
    }

    // ===== validate() - optional fields =====

    @Test
    fun `validate - with installDateTime is accepted`() {
        val firmware = makeValidFirmware() + ("installDateTime" to "2024-01-02T00:00:00Z")
        val response = cmd.validate(mapOf("requestId" to 123, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `validate - all fields valid with full certificate returns null`() {
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
            "signature" to "deadbeef"
        )
        val response = cmd.validate(mapOf("requestId" to 123, "firmware" to firmware))

        assertNull(response)
    }

    // ===== execute() - success path =====

    @Test
    fun `execute - success returns 202 with status sent and correct command`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
    }

    @Test
    fun `execute - success with retries and retryInterval`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf(
            "requestId" to 123,
            "firmware" to firmware,
            "retries" to 5,
            "retryInterval" to 30
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
    }

    @Test
    fun `execute - success without retries and retryInterval`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
    }

    @Test
    fun `execute - success with requestId as Long`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf("requestId" to 999L, "firmware" to firmware))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
        assertEquals(999, gateway.lastRequestId)
    }

    @Test
    fun `execute - success with firmware containing installDateTime`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware() + ("installDateTime" to "2024-01-02T00:00:00Z")
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
    }

    // ===== execute() - failure path =====

    @Test
    fun `execute - rejected response returns 502 with status rejected and error`() {
        gateway.lastResponse = makeCallError()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(502, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `execute - rejected response with retries still returns 502`() {
        gateway.lastResponse = makeCallError()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf(
            "requestId" to 123,
            "firmware" to firmware,
            "retries" to 3,
            "retryInterval" to 60
        ))

        assertEquals(502, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertTrue(entity["error"].toString().contains("rejected", ignoreCase = true))
    }

    // ===== Gateway interaction verification =====

    @Test
    fun `execute - gateway receives correct requestId`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        cmd.execute("CP-001", mapOf("requestId" to 42, "firmware" to firmware))

        assertEquals(42, gateway.lastRequestId)
    }

    @Test
    fun `execute - gateway receives correct chargePointId`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        cmd.execute("CP-999", mapOf("requestId" to 42, "firmware" to firmware))

        assertEquals("CP-999", gateway.lastChargePointId)
    }

    @Test
    fun `execute - gateway receives correct firmware payload`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        cmd.execute("CP-001", mapOf("requestId" to 42, "firmware" to firmware))

        assertEquals(firmware, gateway.lastFirmware)
    }

    @Test
    fun `execute - gateway receives retries when provided`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        cmd.execute("CP-001", mapOf("requestId" to 42, "firmware" to firmware, "retries" to 7, "retryInterval" to 15))

        assertEquals(7, gateway.lastRetries)
        assertEquals(15, gateway.lastRetryInterval)
    }

    @Test
    fun `execute - gateway receives null retries when not provided`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        cmd.execute("CP-001", mapOf("requestId" to 42, "firmware" to firmware))

        assertNull(gateway.lastRetries)
        assertNull(gateway.lastRetryInterval)
    }

    // ===== MUTATION KILLING TESTS =====

    // Kill Line 48 SURVIVED: execute RemoveConditionalMutator_EQUAL_IF on retries
    // When retries is a non-Number, original returns null (safe cast fails), mutant crashes
    @Test
    fun `execute - retries as String still executes and returns accepted`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf(
            "requestId" to 123,
            "firmware" to firmware,
            "retries" to "notANumber"
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
    }

    // Kill Line 49 SURVIVED: execute RemoveConditionalMutator_EQUAL_IF on retryInterval
    // When retryInterval is a non-Number, original returns null, mutant crashes
    @Test
    fun `execute - retryInterval as String still executes and returns accepted`() {
        gateway.lastResponse = makeCallResult()
        val firmware = makeValidFirmware()
        val response = cmd.execute("CP-001", mapOf(
            "requestId" to 123,
            "firmware" to firmware,
            "retryInterval" to "badValue"
        ))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
    }

    // Kill Line 57 SURVIVED mutants (NegateConditionals, NonVoidMethodCall, RemoveConditional)
    // and Line 58 SURVIVED (RemoveConditionalMutator_EQUAL_ELSE on persistenceService?.)
    // by using installDateTime with a spy PersistenceService to capture the parsed value
    @Test
    fun `execute - with installDateTime and persistenceService captures correct parsed instant`() {
        val expectedInstant = Instant.parse("2024-06-15T10:30:00Z")
        val capturedInstallDateTime = mutableListOf<Instant?>()
        val spyPersistenceService = object : PersistenceService() {
            override fun createSignedFirmware(
                chargePointId: String, requestId: Int, location: String,
                retrieveDateTime: Instant, installDateTime: Instant?,
                signingCertificate: String, signature: String
            ): SignedFirmware {
                capturedInstallDateTime.add(installDateTime)
                return SignedFirmware(
                    chargePointId = chargePointId,
                    requestId = requestId,
                    location = location,
                    retrieveDateTime = retrieveDateTime,
                    installDateTime = installDateTime,
                    signingCertificate = signingCertificate,
                    signature = signature,
                    status = "Accepted"
                )
            }
        }

        val gw = object : ChargePointGateway {
            override fun sendSignedUpdateFirmware(
                chargePointId: String, requestId: Int,
                firmware: Map<String, Any>, retries: Int?, retryInterval: Int?
            ): CompletableFuture<OcppMessage> =
                CompletableFuture.completedFuture(OcppMessage.CallResult("id", mapOf()))
            override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
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
            override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
            override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
            override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = TODO()
        }

        val cmdWithPersistence = SignedUpdateFirmwareCommand(gw, spyPersistenceService)
        val firmware = makeValidFirmware() + ("installDateTime" to "2024-06-15T10:30:00Z")
        val response = cmdWithPersistence.execute("CP-001", mapOf("requestId" to 456, "firmware" to firmware))

        assertEquals(202, response.status)
        assertEquals(1, capturedInstallDateTime.size)
        assertEquals(expectedInstant, capturedInstallDateTime[0])
    }

    // Additional test: installDateTime missing should result in null installDateTime in persistence
    // This helps kill RemoveConditionalMutator_EQUAL_IF on line 57 when persistenceService is present
    @Test
    fun `execute - without installDateTime and persistenceService captures null installDateTime`() {
        val capturedInstallDateTime = mutableListOf<Instant?>()
        val spyPersistenceService = object : PersistenceService() {
            override fun createSignedFirmware(
                chargePointId: String, requestId: Int, location: String,
                retrieveDateTime: Instant, installDateTime: Instant?,
                signingCertificate: String, signature: String
            ): SignedFirmware {
                capturedInstallDateTime.add(installDateTime)
                return SignedFirmware(
                    chargePointId = chargePointId,
                    requestId = requestId,
                    location = location,
                    retrieveDateTime = retrieveDateTime,
                    installDateTime = installDateTime,
                    signingCertificate = signingCertificate,
                    signature = signature,
                    status = "Accepted"
                )
            }
        }

        val gw = object : ChargePointGateway {
            override fun sendSignedUpdateFirmware(
                chargePointId: String, requestId: Int,
                firmware: Map<String, Any>, retries: Int?, retryInterval: Int?
            ): CompletableFuture<OcppMessage> =
                CompletableFuture.completedFuture(OcppMessage.CallResult("id", mapOf()))
            override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
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
            override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
            override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = TODO()
            override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
            override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
            override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = TODO()
        }

        val cmdWithPersistence = SignedUpdateFirmwareCommand(gw, spyPersistenceService)
        val firmware = makeValidFirmware()
        val response = cmdWithPersistence.execute("CP-001", mapOf("requestId" to 456, "firmware" to firmware))

        assertEquals(202, response.status)
        assertEquals(1, capturedInstallDateTime.size)
        assertNull(capturedInstallDateTime[0])
    }

    // Kill Line 28 SURVIVED: validateFirmwareFields removed call to Result::constructor-impl
    // When runCatching succeeds, removing the constructor means the lambda result isn't wrapped.
    // We test a valid firmware that passes all validation to exercise the success path.
    @Test
    fun `validate - all fields valid returns null (no error)`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))
        assertNull(response)
    }

    // Kill Line 33 SURVIVED: validateFirmwareFields RemoveConditionalMutator_EQUAL_ELSE
    // on exceptionOrNull()?.let - when no exception, the .let block should not execute.
    // When mutant replaces conditional with false, the .let block always executes.
    // With valid firmware, exceptionOrNull() returns null, and the let block should NOT run.
    // With the mutant, the let block runs on null, causing badRequest(null).
    @Test
    fun `validate - valid firmware does not produce error response`() {
        val firmware = makeValidFirmware()
        val response = cmd.validate(mapOf("requestId" to 1, "firmware" to firmware))
        assertNull(response)
        // If the mutant is active, response would be non-null with null entity
    }

    // Test double
    private class TestMutSecurityGateway : ChargePointGateway {
        var lastResponse: OcppMessage? = OcppMessage.CallResult("default-id", mapOf())
        var lastRequestId: Int? = null
        var lastChargePointId: String? = null
        var lastFirmware: Map<String, Any>? = null
        var lastRetries: Int? = null
        var lastRetryInterval: Int? = null

        override fun sendSignedUpdateFirmware(
            chargePointId: String, requestId: Int,
            firmware: Map<String, Any>, retries: Int?, retryInterval: Int?
        ): CompletableFuture<OcppMessage> {
            lastRequestId = requestId
            lastChargePointId = chargePointId
            lastFirmware = firmware
            lastRetries = retries
            lastRetryInterval = retryInterval
            return CompletableFuture.completedFuture(lastResponse!!)
        }

        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
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
        override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = TODO()
    }

}
