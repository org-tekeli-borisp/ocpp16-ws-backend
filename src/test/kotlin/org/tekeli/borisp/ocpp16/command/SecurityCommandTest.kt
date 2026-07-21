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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("extended-trigger-message", entity["command"])
        assertEquals("SignChargePointCertificate", gateway.lastExtendedTriggerMessage)
    }

    @Test
    fun `ExtendedTriggerMessage - should accept with connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "LogStatusNotification", "connectorId" to 1))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("extended-trigger-message", entity["command"])
        assertEquals(1, gateway.lastExtendedTriggerConnectorId)
    }

    @Test
    fun `ExtendedTriggerMessage - should reject invalid requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.validate(mapOf("requestedMessage" to "InvalidMessage"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestedMessage"))
    }

    @Test
    fun `ExtendedTriggerMessage - should reject missing requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestedMessage"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("install-certificate", entity["command"])
        assertEquals("CentralSystemRootCertificate", gateway.lastCertificateType)
    }

    @Test
    fun `InstallCertificate - should reject invalid certificateType`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "InvalidType", "certificate" to "cert"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificateType"))
    }

    @Test
    fun `InstallCertificate - should reject missing certificate`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificate"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificate"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-installed-certificate-ids", entity["command"])
    }

    @Test
    fun `GetInstalledCertificateIds - should reject invalid certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "InvalidType"))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificateType"))
    }

    @Test
    fun `GetInstalledCertificateIds - should reject missing certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificateType"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("delete-certificate", entity["command"])
    }

    @Test
    fun `DeleteCertificate - should reject missing certificateHashData`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf())

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificateHashData"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("hashAlgorithm"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("issuerNameHash"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
    }

    @Test
    fun `GetLog - should reject invalid logType`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "InvalidLog", "requestId" to 1, "log" to mapOf("remoteLocation" to "url")))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("logType"))
    }

    @Test
    fun `GetLog - should reject missing requestId`() {
        var cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "log" to mapOf("remoteLocation" to "url")))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestId"))
    }

    @Test
    fun `GetLog - should reject missing log`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "requestId" to 1))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("log"))
    }

    @Test
    fun `GetLog - should reject missing remoteLocation`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf("logType" to "SecurityLog", "requestId" to 1, "log" to mapOf<String, Any>()))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("remoteLocation"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("remoteLocation"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("requestId"))
    }

    @Test
    fun `SignedUpdateFirmware - should reject missing firmware`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf("requestId" to 1))

        assertNotNull(response)
        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("firmware"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertNotNull(entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("signingCertificate"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("signature"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("location"))
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("extended-trigger-message", entity["command"])
    }

    @Test
    fun `ExtendedTriggerMessage - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(502, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `ExtendedTriggerMessage - execute should accept with connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "LogStatusNotification", "connectorId" to 1))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("extended-trigger-message", entity["command"])
    }

    @Test
    fun `ExtendedTriggerMessage - execute should accept without connectorId`() {
        gateway.lastResponse = makeCallResult()
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("requestedMessage" to "SignChargePointCertificate"))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("extended-trigger-message", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("install-certificate", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `GetInstalledCertificateIds - execute should return accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-installed-certificate-ids", entity["command"])
    }

    @Test
    fun `GetInstalledCertificateIds - execute should handle rejected response`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateType" to "CentralSystemRootCertificate"))

        assertEquals(502, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("delete-certificate", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-log", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
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
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("signed-update-firmware", entity["command"])
    }

    // Test doubles
    private class TestSecurityGateway : ChargePointGateway {
        var lastResponse: OcppMessage? = OcppMessage.CallResult("id", mapOf())
        var lastExtendedTriggerMessage: String? = null
        var lastExtendedTriggerConnectorId: Int? = null
        var lastCertificateType: String? = null
        var lastCertificateSignedChain: String? = null

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

        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> {
            lastCertificateSignedChain = certificateChain
            return CompletableFuture.completedFuture(lastResponse!!)
        }

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

    // ---- Mutation kill tests: ExtendedTriggerMessage empty requestedMessage ----

    @Test
    fun `ExtendedTriggerMessage - should reject empty requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val response = cmd.validate(mapOf("requestedMessage" to ""))

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: TriggerMessage all valid message types ----

    @Test
    fun `TriggerMessageCommand validate accepts all valid messages`() {
        val cmd = TriggerMessageCommand(TestSecurityGateway())
        val validMessages = listOf(
            "BootNotification", "DiagnosticsStatusNotification",
            "FirmwareStatusNotification", "Heartbeat",
            "MeterValues", "StatusNotification",
            "LogStatusNotification", "SignChargePointCertificate"
        )
        for (msg in validMessages) {
            val resp = cmd.validate(mapOf("requestedMessage" to msg))
            assertNull(resp, "Should accept: $msg")
        }
    }

    @Test
    fun `ExtendedTriggerMessageCommand validate accepts all valid messages`() {
        val cmd = ExtendedTriggerMessageCommand(TestSecurityGateway())
        val validMessages = listOf(
            "BootNotification", "LogStatusNotification",
            "FirmwareStatusNotification", "Heartbeat",
            "MeterValues", "SignChargePointCertificate",
            "StatusNotification"
        )
        for (msg in validMessages) {
            val resp = cmd.validate(mapOf("requestedMessage" to msg))
            assertNull(resp, "Should accept: $msg")
        }
    }

    // ---- Mutation kill tests: InstallCertificate boundary ----

    @Test
    fun `InstallCertificate - should accept certificate at exactly 5500 chars`() {
        val cmd = InstallCertificateCommand(gateway)
        val cert = "A".repeat(5500)
        val response = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to cert))
        assertNull(response)
    }

    @Test
    fun `InstallCertificate - should reject certificate at 5501 chars`() {
        val cmd = InstallCertificateCommand(gateway)
        val cert = "A".repeat(5501)
        val response = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to cert))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `InstallCertificate - should accept ManufacturerRootCertificate`() {
        val cmd = InstallCertificateCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "ManufacturerRootCertificate", "certificate" to "cert"))
        assertNull(response)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware boundary ----

    @Test
    fun `SignedUpdateFirmware - should accept location exactly 512 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val loc = "h".repeat(512)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to loc,
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNull(response)
    }

    @Test
    fun `SignedUpdateFirmware - should reject location 513 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val loc = "h".repeat(513)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to loc,
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should accept signingCertificate 5500 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val cert = "c".repeat(5500)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to cert,
                "signature" to "sig"
            )
        ))
        assertNull(response)
    }

    @Test
    fun `SignedUpdateFirmware - should reject signingCertificate 5501 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val cert = "c".repeat(5501)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to cert,
                "signature" to "sig"
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `SignedUpdateFirmware - should accept signature 800 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val sig = "s".repeat(800)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to sig
            )
        ))
        assertNull(response)
    }

    @Test
    fun `SignedUpdateFirmware - should reject signature 801 chars`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val sig = "s".repeat(801)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to sig
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware non-Number requestId ----

    @Test
    fun `SignedUpdateFirmware - should reject non-Number requestId`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["requestId"] = false as Any?
        payload["firmware"] = mapOf(
            "location" to "http://fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        @Suppress("UNCHECKED_CAST")
        val response = cmd.validate(payload as Map<String, Any>)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware empty location ----

    @Test
    fun `SignedUpdateFirmware - should reject empty location`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware empty signingCertificate ----

    @Test
    fun `SignedUpdateFirmware - should reject empty signingCertificate`() {
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.validate(mapOf(
            "requestId" to 1,
            "firmware" to mapOf(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "",
                "signature" to "sig"
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware with installDateTime ----

    @Test
    fun `SignedUpdateFirmware - execute with installDateTime returns accepted`() {
        gateway.lastResponse = makeCallResult()
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val firmware = mapOf(
            "location" to "https://example.com/fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "installDateTime" to "2024-01-02T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        val response = cmd.execute("CP-1", mapOf("requestId" to 1, "firmware" to firmware))
        assertEquals(202, response.status)
    }

    // ---- Mutation kill tests: SignedUpdateFirmware BAD_GATEWAY on CallError ----

    @Test
    fun `SignedUpdateFirmware - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val response = cmd.execute(
            "CP-1", mapOf(
                "requestId" to 1,
                "firmware" to mapOf(
                    "location" to "http://fw.bin",
                    "retrieveDateTime" to "2024-01-01T00:00:00Z",
                    "signingCertificate" to "cert",
                    "signature" to "sig"
                )
            )
        )
        assertEquals(502, response.status)
    }

    // ---- Mutation kill tests: GetLog validate ----

    @Test
    fun `GetLog - should reject non-Map log`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "DiagnosticsLog", "requestId" to 1,
            "log" to "not-a-map"
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject empty remoteLocation`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "SecurityLog", "requestId" to 1,
            "log" to mapOf("remoteLocation" to "")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject non-Number requestId`() {
        val cmd = GetLogCommand(gateway)
        val response = cmd.validate(mapOf(
            "logType" to "DiagnosticsLog", "requestId" to "not-a-number",
            "log" to mapOf("remoteLocation" to "http://example.com/log")
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `GetLog - should reject null logType`() {
        val cmd = GetLogCommand(gateway)
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["logType"] = null as Any?
        payload["requestId"] = 1
        payload["log"] = mapOf("remoteLocation" to "http://example.com/log")
        @Suppress("UNCHECKED_CAST")
        val response = cmd.validate(payload as Map<String, Any>)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: DeleteCertificate ----

    @Test
    fun `DeleteCertificate - should accept valid data`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf(
            "certificateHashData" to mapOf(
                "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
                "issuerKeyHash" to "h2", "serialNumber" to "s1"
            )
        ))
        assertNull(response)
    }

    @Test
    fun `DeleteCertificate - should reject empty issuerNameHash`() {
        val cmd = DeleteCertificateCommand(gateway)
        val response = cmd.validate(mapOf(
            "certificateHashData" to mapOf(
                "hashAlgorithm" to "SHA256", "issuerNameHash" to "",
                "issuerKeyHash" to "h2", "serialNumber" to "s1"
            )
        ))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `DeleteCertificate - should reject null issuerKeyHash`() {
        val cmd = DeleteCertificateCommand(gateway)
        val hashData: MutableMap<String, Any?> = mutableMapOf(
            "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
            "serialNumber" to "s1"
        )
        hashData["issuerKeyHash"] = null as Any?
        val response = cmd.validate(mapOf("certificateHashData" to hashData))
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `DeleteCertificate - should reject null certificateHashData`() {
        val cmd = DeleteCertificateCommand(gateway)
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["certificateHashData"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val response = cmd.validate(payload as Map<String, Any>)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: InstallCertificate null certificate ----

    @Test
    fun `InstallCertificate - should reject null certificate`() {
        val cmd = InstallCertificateCommand(gateway)
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["certificateType"] = "CentralSystemRootCertificate"
        payload["certificate"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val response = cmd.validate(payload as Map<String, Any>)
        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // ---- Mutation kill tests: GetInstalledCertificateIds valid types ----

    @Test
    fun `GetInstalledCertificateIds - should accept CentralSystemRootCertificate`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate"))
        assertNull(response)
    }

    @Test
    fun `GetInstalledCertificateIds - should accept ManufacturerRootCertificate`() {
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val response = cmd.validate(mapOf("certificateType" to "ManufacturerRootCertificate"))
        assertNull(response)
    }

    // ---- Mutation kill tests: RemoteStartTransaction null idTag ----

    @Test
    fun `RemoteStartTransactionCommand validate with null idTag`() {
        val cmd = RemoteStartTransactionCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["idTag"] = null as Any?
        payload["connectorId"] = 1
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    // ---- Mutation kill tests: non-Number connectorId for standard commands ----

    @Test
    fun `GetCompositeScheduleCommand validate with non-Number connectorId`() {
        val cmd = GetCompositeScheduleCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("connectorId" to "not-a-number", "duration" to 300))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with non-Number duration`() {
        val cmd = GetCompositeScheduleCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("connectorId" to 1, "duration" to "not-a-number"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate with non-Number connectorId`() {
        val cmd = ReserveNowCommand(TestSecurityGateway())
        val resp = cmd.validate(
            mapOf(
                "connectorId" to "bad", "expiryDate" to "2024-01-01T00:00:00Z",
                "idTag" to "CARD1", "reservationId" to 1
            )
        )
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate with non-Number reservationId`() {
        val cmd = ReserveNowCommand(TestSecurityGateway())
        val resp = cmd.validate(
            mapOf(
                "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z",
                "idTag" to "CARD1", "reservationId" to "bad"
            )
        )
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with non-Number listVersion`() {
        val cmd = SendLocalListCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("listVersion" to "bad", "updateType" to "Full"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `ChangeAvailabilityCommand validate with non-Number connectorId`() {
        val cmd = ChangeAvailabilityCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad", "type" to "Inoperative"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with non-Number connectorId`() {
        val cmd = SetChargingProfileCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad", "csChargingProfiles" to mapOf<String, Any>()))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with null csChargingProfiles`() {
        val cmd = SetChargingProfileCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf<String, Any>("connectorId" to 1))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `UnlockConnectorCommand validate with non-Number connectorId`() {
        val cmd = UnlockConnectorCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `RemoteStopTransactionCommand validate with non-Number transactionId`() {
        val cmd = RemoteStopTransactionCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("transactionId" to "bad"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `CancelReservationCommand validate with non-Number reservationId`() {
        val cmd = CancelReservationCommand(TestSecurityGateway())
        val resp = cmd.validate(mapOf("reservationId" to "bad"))
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    // ---- Mutation kill tests: null fields ----

    @Test
    fun `ChangeAvailabilityCommand validate with null type`() {
        val cmd = ChangeAvailabilityCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["connectorId"] = 1
        payload["type"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with null updateType`() {
        val cmd = SendLocalListCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["listVersion"] = 1
        payload["updateType"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with null duration`() {
        val cmd = GetCompositeScheduleCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["connectorId"] = 1
        payload["duration"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with null connectorId`() {
        val cmd = GetCompositeScheduleCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["connectorId"] = null as Any?
        payload["duration"] = 300
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `RemoteStartTransactionCommand validate with null connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["idTag"] = "TAG1"
        payload["connectorId"] = null as Any?
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with null connectorId`() {
        val cmd = SetChargingProfileCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["connectorId"] = null as Any?
        payload["csChargingProfiles"] = mapOf<String, Any>()
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `ChangeAvailabilityCommand validate with null connectorId`() {
        val cmd = ChangeAvailabilityCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["connectorId"] = null as Any?
        payload["type"] = "Inoperative"
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with null listVersion`() {
        val cmd = SendLocalListCommand(TestSecurityGateway())
        val payload: MutableMap<String, Any?> = LinkedHashMap()
        payload["listVersion"] = null as Any?
        payload["updateType"] = "Full"
        @Suppress("UNCHECKED_CAST")
        val resp = cmd.validate(payload as Map<String, Any>)
        assertNotNull(resp)
        assertEquals(400, resp!!.status)
    }

    // ---- Mutation kill tests: command execute BAD_GATEWAY for security commands ----

    @Test
    fun `InstallCertificate - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = InstallCertificateCommand(gateway)
        val resp = cmd.execute(
            "CP-1",
            mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to "cert-data")
        )
        assertEquals(502, resp.status)
    }

    @Test
    fun `GetInstalledCertificateIds - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val resp = cmd.execute("CP-1", mapOf("certificateType" to "CentralSystemRootCertificate"))
        assertEquals(502, resp.status)
    }

    @Test
    fun `DeleteCertificate - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = DeleteCertificateCommand(gateway)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "certificateHashData" to mapOf(
                    "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
                    "issuerKeyHash" to "h2", "serialNumber" to "s1"
                )
            )
        )
        assertEquals(502, resp.status)
    }

    @Test
    fun `GetLog - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = GetLogCommand(gateway)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "logType" to "DiagnosticsLog", "requestId" to 1,
                "log" to mapOf("remoteLocation" to "http://example.com/log")
            )
        )
        assertEquals(502, resp.status)
    }

    // ---- Mutation kill tests: command execute ACCEPTED for security commands ----

    @Test
    fun `InstallCertificate - execute returns ACCEPTED on CallResult`() {
        gateway.lastResponse = makeCallResult()
        val cmd = InstallCertificateCommand(gateway)
        val resp = cmd.execute(
            "CP-1",
            mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to "cert-data")
        )
        assertEquals(202, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
    }

    @Test
    fun `GetInstalledCertificateIds - execute returns ACCEPTED on CallResult`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetInstalledCertificateIdsCommand(gateway)
        val resp = cmd.execute("CP-1", mapOf("certificateType" to "CentralSystemRootCertificate"))
        assertEquals(202, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
    }

    @Test
    fun `DeleteCertificate - execute returns ACCEPTED on CallResult`() {
        gateway.lastResponse = makeCallResult()
        val cmd = DeleteCertificateCommand(gateway)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "certificateHashData" to mapOf(
                    "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
                    "issuerKeyHash" to "h2", "serialNumber" to "s1"
                )
            )
        )
        assertEquals(202, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
    }

    @Test
    fun `GetLog - execute returns ACCEPTED on CallResult`() {
        gateway.lastResponse = makeCallResult()
        val cmd = GetLogCommand(gateway)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "logType" to "SecurityLog", "requestId" to 1,
                "log" to mapOf("remoteLocation" to "https://example.com/logs")
            )
        )
        assertEquals(202, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
    }

    @Test
    fun `SignedUpdateFirmware - execute returns ACCEPTED on CallResult`() {
        gateway.lastResponse = makeCallResult()
        val cmd = SignedUpdateFirmwareCommand(gateway)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "requestId" to 1,
                "firmware" to mapOf(
                    "location" to "http://fw.bin",
                    "retrieveDateTime" to "2024-01-01T00:00:00Z",
                    "signingCertificate" to "cert",
                    "signature" to "sig"
                )
            )
        )
        assertEquals(202, resp.status)
    }

    // ---- Mutation kill tests: ExtendedTriggerMessage execute BAD_GATEWAY ----

    @Test
    fun `ExtendedTriggerMessage - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = ExtendedTriggerMessageCommand(gateway)
        val resp = cmd.execute("CP-1", mapOf("requestedMessage" to "SignChargePointCertificate"))
        assertEquals(502, resp.status)
    }

    // ---- CertificateSignedCommand tests ----

    @Test
    fun `CertificateSigned - has correct name`() {
        val cmd = CertificateSignedCommand(gateway)
        assertEquals("send-certificate-signed", cmd.name)
    }

    @Test
    fun `CertificateSigned - should accept valid certificateChain`() {
        gateway.lastResponse = makeCallResult()
        gateway.lastCertificateSignedChain = null
        val cmd = CertificateSignedCommand(gateway)
        val response = cmd.execute("CP-001", mapOf("certificateChain" to "MIIBkTCB..."))

        assertEquals(202, response.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertEquals("sent", entity["status"])
        assertEquals("send-certificate-signed", entity["command"])
        assertEquals("MIIBkTCB...", gateway.lastCertificateSignedChain)
    }

    @Test
    fun `CertificateSigned - should reject missing certificateChain`() {
        val cmd = CertificateSignedCommand(gateway)
        val response = cmd.validate(mapOf<String, Any>())

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("certificateChain"))
    }

    @Test
    fun `CertificateSigned - should reject empty certificateChain`() {
        val cmd = CertificateSignedCommand(gateway)
        val response = cmd.validate(mapOf("certificateChain" to ""))

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `CertificateSigned - should reject certificateChain exceeding max length`() {
        val cmd = CertificateSignedCommand(gateway)
        val longChain = "X".repeat(10001)
        val response = cmd.validate(mapOf("certificateChain" to longChain))

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    @Test
    fun `CertificateSigned - execute returns BAD_GATEWAY on CallError`() {
        gateway.lastResponse = OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.GENERIC_ERROR, "Error", null)
        val cmd = CertificateSignedCommand(gateway)
        val resp = cmd.execute("CP-1", mapOf("certificateChain" to "MIIBkTCB..."))
        assertEquals(502, resp.status)
    }
}
