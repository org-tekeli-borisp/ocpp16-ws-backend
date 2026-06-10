package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.util.concurrent.CompletableFuture

class SecurityCommandTest {

    private val gateway = TestSecurityGateway()

    private fun makeCallResult() = OcppMessage.CallResult("id", mapOf())

    // ExtendedTriggerMessageCommand tests
    @Test
    fun `ExtendedTriggerMessage - should accept valid message`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(202, response.status)
        assertEquals("SignChargePointCertificate", gateway.lastExtendedTriggerMessage)
    }

    @Test
    fun `ExtendedTriggerMessage - should accept with connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "LogStatusNotification", "connectorId" to 1))

        assertEquals(202, response.status)
        assertEquals(1, gateway.lastExtendedTriggerConnectorId)
    }

    @Test
    fun `ExtendedTriggerMessage - should reject invalid requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.validate(mapOf("requestedMessage" to "InvalidMessage"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `ExtendedTriggerMessage - should reject missing requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `ExtendedTriggerMessage - should accept all valid message types`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val validMessages = listOf(
            "BootNotification", "LogStatusNotification", "FirmwareStatusNotification",
            "Heartbeat", "MeterValues", "SignChargePointCertificate", "StatusNotification"
        )

        for (msg in validMessages) {
            val response = cmd.validate(mapOf("requestedMessage" to msg))
            assertNull(response, "Should accept message: $msg")
        }
    }

    // InstallCertificateCommand tests
    @Test
    fun `InstallCertificate - should accept valid certificate`() {
        gateway.lastResponse = makeCallResult()
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "certificateType" to "CentralSystemRootCertificate",
            "certificate" to "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----"
        ))

        assertEquals(202, response.status)
        assertEquals("CentralSystemRootCertificate", gateway.lastCertificateType)
    }

    @Test
    fun `InstallCertificate - should reject invalid certificateType`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "InvalidType", "certificate" to "cert"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `InstallCertificate - should reject missing certificate`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `InstallCertificate - should reject certificate exceeding 5500 chars`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf(
            "certificateType" to "CentralSystemRootCertificate",
            "certificate" to "A".repeat(5501)
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `InstallCertificate - should accept both certificate types`() {
        val cmd = InstallCertificateCommand(gateway)

        val r1 = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to "cert"))
        assertNull(r1)

        val r2 = cmd.validate(mapOf("certificateType" to "ManufacturerRootCertificate", "certificate" to "cert"))
        assertNull(r2)
    }

    // GetInstalledCertificateIdsCommand tests
    @Test
    fun `GetInstalledCertificateIds - should accept valid request`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetInstalledCertificateIds - should reject invalid certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "InvalidType"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetInstalledCertificateIds - should reject missing certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // DeleteCertificateCommand tests
    @Test
    fun `DeleteCertificate - should accept valid request`() {
        gateway.lastResponse = makeCallResult()
        val cmd = DeleteCertificateCommand(gateway)
        val hashData = mapOf(
            "hashAlgorithm" to "SHA256",
            "issuerNameHash" to "abc123",
            "issuerKeyHash" to "def456",
            "serialNumber" to "1234"
        )
        val response = cmd.execute("CP-001", mapOf("certificateHashData" to hashData))

        assertEquals(202, response.status)
    }

    @Test
    fun `DeleteCertificate - should reject missing certificateHashData`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `DeleteCertificate - should reject invalid hashAlgorithm`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf(
            "certificateHashData" to mapOf("hashAlgorithm" to "MD5")
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `DeleteCertificate - should reject missing issuerNameHash`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf(
            "certificateHashData" to mapOf("hashAlgorithm" to "SHA256")
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `DeleteCertificate - should accept all hash algorithms`() {
        val cmd = DeleteCertificateCommand(gateway)
        val baseData = mapOf(
            "issuerNameHash" to "abc",
            "issuerKeyHash" to "def",
            "serialNumber" to "123"
        )

        for (algo in listOf("SHA256", "SHA384", "SHA512")) {
            val response = cmd.validate(mapOf(
                "certificateHashData" to (baseData + ("hashAlgorithm" to algo))
            ))
            assertNull(response, "Should accept algorithm: $algo")
        }
    }

    // GetLogCommand tests
    @Test
    fun `GetLog - should accept valid SecurityLog request`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetLog - should accept valid DiagnosticsLog request`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "DiagnosticsLog",
            "requestId" to 456,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetLog - should reject invalid logType`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "InvalidLog", "requestId" to 1, "log" to mapOf("remoteLocation" to "url")))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject missing requestId`() {
        var cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "log" to mapOf("remoteLocation" to "url")))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject missing log`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "requestId" to 1))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject missing remoteLocation`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "requestId" to 1, "log" to mapOf<String, Any>()))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject remoteLocation exceeding 512 chars`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog",
            "requestId" to 1,
            "log" to mapOf("remoteLocation" to "A".repeat(513))
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // SignedUpdateFirmwareCommand tests
    @Test
    fun `SignedUpdateFirmware - validate should accept valid request`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
            "signature" to "deadbeef"
        )
        val response = cmd.validate(mapOf("requestId" to 123, "firmware" to firmware))

        assertNull(response)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing requestId`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf("firmware" to mapOf("location" to "url")))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing firmware`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf("requestId" to 1))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing location`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>()
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing retrieveDateTime`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf("location" to "url")
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing signingCertificate`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf("location" to "url", "retrieveDateTime" to "2024-01-01T00:00:00Z")
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing signature`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "url",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert"
            )
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject signingCertificate exceeding 5500 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "url",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "A".repeat(5501),
                "signature" to "sig"
            )
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject signature exceeding 800 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "url",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "A".repeat(801)
            )
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should reject location exceeding 512 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "A".repeat(513),
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should accept with optional installDateTime`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "installDateTime" to "2024-01-02T00:00:00Z",
            "signingCertificate" to "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
            "signature" to "deadbeef"
        )
        val response = cmd.validate(mapOf("requestId" to 123, "firmware" to firmware))

        assertNull(response)
    }

    // Execute tests for all commands
    @Test
    fun `ExtendedTriggerMessage - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(202, response.status)
    }

    @Test
    fun `ExtendedTriggerMessage - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(502, response.status)
    }

    @Test
    fun `ExtendedTriggerMessage - execute should accept with connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "LogStatusNotification", "connectorId" to 1))

        assertEquals(202, response.status)
    }

    @Test
    fun `ExtendedTriggerMessage - execute should accept without connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(202, response.status)
    }

    @Test
    fun `InstallCertificate - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "certificateType" to "CentralSystemRootCertificate",
            "certificate" to "cert"
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `InstallCertificate - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "certificateType" to "CentralSystemRootCertificate",
            "certificate" to "cert"
        ))

        assertEquals(502, response.status)
    }

    @Test
    fun `GetInstalledCertificateIds - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetInstalledCertificateIds - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertEquals(502, response.status)
    }

    @Test
    fun `DeleteCertificate - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = DeleteCertificateCommand(gateway)
        val hashData = mapOf(
            "hashAlgorithm" to "SHA256",
            "issuerNameHash" to "abc",
            "issuerKeyHash" to "def",
            "serialNumber" to "123"
        )
        val response = cmd.execute("CP-001", mapOf("certificateHashData" to hashData))

        assertEquals(202, response.status)
    }

    @Test
    fun `DeleteCertificate - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = DeleteCertificateCommand(gateway)
        val hashData = mapOf(
            "hashAlgorithm" to "SHA256",
            "issuerNameHash" to "abc",
            "issuerKeyHash" to "def",
            "serialNumber" to "123"
        )
        val response = cmd.execute("CP-001", mapOf("certificateHashData" to hashData))

        assertEquals(502, response.status)
    }

    @Test
    fun `GetLog - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetLog - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(502, response.status)
    }

    @Test
    fun `GetLog - execute should accept with retries and retryInterval`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs"),
            "retries" to 3,
            "retryInterval" to 60
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `GetLog - execute should accept without retries and retryInterval`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val response = cmd.execute("CP-001", mapOf(
            "logType" to "SecurityLog",
            "requestId" to 123,
            "log" to mapOf("remoteLocation" to "https://example.com/logs")
        ))

        assertEquals(202, response.status)
    }

    @Test
    fun `SignedUpdateFirmware - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(202, response.status)
    }

    @Test
    fun `SignedUpdateFirmware - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware))

        assertEquals(502, response.status)
    }

    @Test
    fun `SignedUpdateFirmware - execute should accept with retries and retryInterval`() {
        gateway.lastResponse = makeCallResult()
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        val response = cmd.execute("CP-001", mapOf("requestId" to 123, "firmware" to firmware, "retries" to 3, "retryInterval" to 60))

        assertEquals(202, response.status)
    }

    // Test doubles
    private class TestSecurityGateway : ChargePointGateway {
        var lastResponse: OcppMessage? = OcppMessage.CallResult("id", mapOf())
        var lastExtendedTriggerMessage: String? = null
        var lastExtendedTriggerConnectorId: Int? = null
        var lastCertificateType: String? = null

        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> {
            lastExtendedTriggerMessage = requestedMessage
            lastExtendedTriggerConnectorId = connectorId
            return CompletableFuture.completedFuture(lastResponse!!)
        }

        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> {
            lastCertificateType = certificateType
            return CompletableFuture.completedFuture(lastResponse!!)
        }

        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(lastResponse!!)

        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(lastResponse!!)

        override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(lastResponse!!)

        override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(lastResponse!!)

        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(lastResponse!!)

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
    }
}
