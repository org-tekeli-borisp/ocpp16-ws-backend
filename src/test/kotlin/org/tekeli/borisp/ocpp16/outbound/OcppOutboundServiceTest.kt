package org.tekeli.borisp.ocpp16.outbound

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import java.util.concurrent.CompletableFuture

class OcppOutboundServiceTest {

    private val testRegistry = TestRegistry()
    private lateinit var service: OcppOutboundService

    @BeforeEach
    fun setup() {
        service = OcppOutboundService()
        service.chargePointRegistry = testRegistry
    }

    @Test
    fun `sendReset delegates to registry`() {
        service.sendReset("CP-001", "Hard")

        assertEquals("CP-001", testRegistry.lastChargePointId)
        assertEquals("Reset", testRegistry.lastAction)
        assertEquals("Hard", testRegistry.lastPayload?.get("type"))
    }

    @Test
    fun `sendClearCache delegates with null payload`() {
        service.sendClearCache("CP-001")

        assertEquals("ClearCache", testRegistry.lastAction)
        assertNull(testRegistry.lastPayload)
    }

    @Test
    fun `sendChangeConfiguration delegates correctly`() {
        service.sendChangeConfiguration("CP-001", "Key1", "Value1")

        assertEquals("ChangeConfiguration", testRegistry.lastAction)
        assertEquals("Key1", testRegistry.lastPayload?.get("key"))
        assertEquals("Value1", testRegistry.lastPayload?.get("value"))
    }

    @Test
    fun `sendChangeAvailability delegates correctly`() {
        service.sendChangeAvailability("CP-001", 1, "Inoperative")

        assertEquals("ChangeAvailability", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
        assertEquals("Inoperative", testRegistry.lastPayload?.get("type"))
    }

    @Test
    fun `sendGetConfiguration with keys passes payload`() {
        service.sendGetConfiguration("CP-001", listOf("Key1", "Key2"))

        assertEquals("GetConfiguration", testRegistry.lastAction)
        assertEquals(listOf("Key1", "Key2"), testRegistry.lastPayload?.get("key"))
    }

    @Test
    fun `sendGetConfiguration with null keys passes null`() {
        service.sendGetConfiguration("CP-001", null)

        assertEquals("GetConfiguration", testRegistry.lastAction)
        assertNull(testRegistry.lastPayload)
    }

    @Test
    fun `sendRemoteStopTransaction delegates correctly`() {
        service.sendRemoteStopTransaction("CP-001", 42)

        assertEquals("RemoteStopTransaction", testRegistry.lastAction)
        assertEquals(42, testRegistry.lastPayload?.get("transactionId"))
    }

    @Test
    fun `sendTriggerMessage without connectorId`() {
        service.sendTriggerMessage("CP-001", "Heartbeat", null)

        assertEquals("TriggerMessage", testRegistry.lastAction)
        assertEquals("Heartbeat", testRegistry.lastPayload?.get("requestedMessage"))
        assertFalse(testRegistry.lastPayload?.containsKey("connectorId") == true)
    }

    @Test
    fun `sendTriggerMessage with connectorId`() {
        service.sendTriggerMessage("CP-001", "Heartbeat", 1)

        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
    }

    @Test
    fun `sendUnlockConnector delegates correctly`() {
        service.sendUnlockConnector("CP-001", 3)

        assertEquals("UnlockConnector", testRegistry.lastAction)
        assertEquals(3, testRegistry.lastPayload?.get("connectorId"))
    }

    @Test
    fun `sendUpdateFirmware delegates with all params`() {
        service.sendUpdateFirmware("CP-001", "https://example.com/fw", "2024-01-01T00:00:00Z", 3, 60)

        assertEquals("UpdateFirmware", testRegistry.lastAction)
        assertEquals("https://example.com/fw", testRegistry.lastPayload?.get("location"))
        assertEquals(3, testRegistry.lastPayload?.get("retries"))
        assertEquals(60, testRegistry.lastPayload?.get("retryInterval"))
    }

    @Test
    fun `sendUpdateFirmware filters null retries`() {
        service.sendUpdateFirmware("CP-001", "https://example.com/fw", "2024-01-01T00:00:00Z", null, null)

        assertFalse(testRegistry.lastPayload?.containsKey("retries") == true)
        assertFalse(testRegistry.lastPayload?.containsKey("retryInterval") == true)
    }

    @Test
    fun `sendGetDiagnostics delegates with filtered payload`() {
        service.sendGetDiagnostics("CP-001", "https://example.com/diag", 2, 30, null, null)

        assertEquals("GetDiagnostics", testRegistry.lastAction)
        assertEquals("https://example.com/diag", testRegistry.lastPayload?.get("location"))
        assertEquals(2, testRegistry.lastPayload?.get("retries"))
    }

    @Test
    fun `sendGetDiagnostics filters null retries`() {
        service.sendGetDiagnostics("CP-001", "https://example.com/diag", null, null, null, null)

        assertFalse(testRegistry.lastPayload?.containsKey("retries") == true)
    }

    @Test
    fun `sendGetLocalListVersion delegates correctly`() {
        service.sendGetLocalListVersion("CP-001")

        assertEquals("GetLocalListVersion", testRegistry.lastAction)
        assertNull(testRegistry.lastPayload)
    }

    @Test
    fun `sendRemoteStartTransaction with idTag only`() {
        service.sendRemoteStartTransaction("CP-001", "CARD1", null)

        assertEquals("RemoteStartTransaction", testRegistry.lastAction)
        assertEquals("CARD1", testRegistry.lastPayload?.get("idTag"))
        assertFalse(testRegistry.lastPayload?.containsKey("connectorId") == true)
    }

    @Test
    fun `sendRemoteStartTransaction with connectorId`() {
        service.sendRemoteStartTransaction("CP-001", "CARD1", 2)

        assertEquals(2, testRegistry.lastPayload?.get("connectorId"))
    }

    @Test
    fun `sendReserveNow delegates correctly`() {
        service.sendReserveNow("CP-001", 1, "2024-01-01T00:00:00Z", "CARD1", 42)

        assertEquals("ReserveNow", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
        assertEquals("CARD1", testRegistry.lastPayload?.get("idTag"))
    }

    @Test
    fun `sendCancelReservation delegates correctly`() {
        service.sendCancelReservation("CP-001", 42)

        assertEquals("CancelReservation", testRegistry.lastAction)
        assertEquals(42, testRegistry.lastPayload?.get("reservationId"))
    }

    @Test
    fun `sendSendLocalList delegates correctly`() {
        service.sendSendLocalList("CP-001", 1, "Update")

        assertEquals("SendLocalList", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("listVersion"))
        assertEquals("Update", testRegistry.lastPayload?.get("updateType"))
    }

    @Test
    fun `sendSetChargingProfile delegates correctly`() {
        val profiles = mapOf<String, Any>("chargingProfile" to mapOf("stackLevel" to 1))
        service.sendSetChargingProfile("CP-001", 1, profiles)

        assertEquals("SetChargingProfile", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
        assertEquals(profiles, testRegistry.lastPayload?.get("csChargingProfiles"))
    }

    @Test
    fun `sendClearChargingProfile with all params`() {
        service.sendClearChargingProfile("CP-001", 1, 5)

        assertEquals("ClearChargingProfile", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
        assertEquals(5, testRegistry.lastPayload?.get("stackLevel"))
    }

    @Test
    fun `sendClearChargingProfile with nulls filters them`() {
        service.sendClearChargingProfile("CP-001", null, null)

        assertFalse(testRegistry.lastPayload?.containsKey("connectorId") == true)
        assertFalse(testRegistry.lastPayload?.containsKey("stackLevel") == true)
    }

    @Test
    fun `sendGetCompositeSchedule delegates correctly`() {
        service.sendGetCompositeSchedule("CP-001", 1, 3600)

        assertEquals("GetCompositeSchedule", testRegistry.lastAction)
        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
        assertEquals(3600, testRegistry.lastPayload?.get("duration"))
    }

    @Test
    fun `sendExtendedTriggerMessage without connectorId`() {
        service.sendExtendedTriggerMessage("CP-001", "SignChargePointCertificate", null)

        assertEquals("ExtendedTriggerMessage", testRegistry.lastAction)
        assertEquals("SignChargePointCertificate", testRegistry.lastPayload?.get("requestedMessage"))
        assertFalse(testRegistry.lastPayload?.containsKey("connectorId") == true)
    }

    @Test
    fun `sendExtendedTriggerMessage with connectorId`() {
        service.sendExtendedTriggerMessage("CP-001", "LogStatusNotification", 1)

        assertEquals(1, testRegistry.lastPayload?.get("connectorId"))
    }

    @Test
    fun `sendInstallCertificate delegates correctly`() {
        service.sendInstallCertificate("CP-001", "CentralSystemRootCertificate", "cert-data")

        assertEquals("InstallCertificate", testRegistry.lastAction)
        assertEquals("CentralSystemRootCertificate", testRegistry.lastPayload?.get("certificateType"))
        assertEquals("cert-data", testRegistry.lastPayload?.get("certificate"))
    }

    @Test
    fun `sendGetInstalledCertificateIds delegates correctly`() {
        service.sendGetInstalledCertificateIds("CP-001", "ManufacturerRootCertificate")

        assertEquals("GetInstalledCertificateIds", testRegistry.lastAction)
        assertEquals("ManufacturerRootCertificate", testRegistry.lastPayload?.get("certificateType"))
    }

    @Test
    fun `sendDeleteCertificate delegates correctly`() {
        val hashData = mapOf("hashAlgorithm" to "SHA256", "serialNumber" to "123")
        service.sendDeleteCertificate("CP-001", hashData)

        assertEquals("DeleteCertificate", testRegistry.lastAction)
        assertEquals(hashData, testRegistry.lastPayload?.get("certificateHashData"))
    }

    @Test
    fun `sendGetLog with all params`() {
        val log = mapOf("remoteLocation" to "https://example.com/logs")
        service.sendGetLog("CP-001", "SecurityLog", 123, log, 3, 60)

        assertEquals("GetLog", testRegistry.lastAction)
        assertEquals("SecurityLog", testRegistry.lastPayload?.get("logType"))
        assertEquals(123, testRegistry.lastPayload?.get("requestId"))
        assertEquals(3, testRegistry.lastPayload?.get("retries"))
        assertEquals(60, testRegistry.lastPayload?.get("retryInterval"))
    }

    @Test
    fun `sendGetLog without retries filters nulls`() {
        val log = mapOf("remoteLocation" to "https://example.com/logs")
        service.sendGetLog("CP-001", "DiagnosticsLog", 456, log, null, null)

        assertFalse(testRegistry.lastPayload?.containsKey("retries") == true)
    }

    @Test
    fun `sendSignedUpdateFirmware delegates correctly`() {
        val firmware = mapOf("location" to "https://example.com/fw", "retrieveDateTime" to "2024-01-01T00:00:00Z")
        service.sendSignedUpdateFirmware("CP-001", 123, firmware, 3, 60)

        assertEquals("SignedUpdateFirmware", testRegistry.lastAction)
        assertEquals(123, testRegistry.lastPayload?.get("requestId"))
        assertEquals(firmware, testRegistry.lastPayload?.get("firmware"))
    }

    @Test
    fun `sendSignedUpdateFirmware filters null retries`() {
        val firmware = mapOf("location" to "https://example.com/fw", "retrieveDateTime" to "2024-01-01T00:00:00Z")
        service.sendSignedUpdateFirmware("CP-001", 123, firmware, null, null)

        assertFalse(testRegistry.lastPayload?.containsKey("retries") == true)
    }

    @Test
    fun `sendCertificateSigned delegates correctly`() {
        service.sendCertificateSigned("CP-001", "cert-chain-data")

        assertEquals("CertificateSigned", testRegistry.lastAction)
        assertEquals("cert-chain-data", testRegistry.lastPayload?.get("certificateChain"))
    }

    private inner class TestRegistry : ChargePointRegistry() {
        var lastChargePointId: String? = null
        var lastAction: String? = null
        var lastPayload: Map<String, Any>? = null
        var response: OcppMessage = OcppMessage.CallResult("id", mapOf())

        override fun sendCall(chargePointId: String, action: String, payload: Map<String, Any>?): CompletableFuture<OcppMessage> {
            lastChargePointId = chargePointId
            lastAction = action
            lastPayload = payload
            return CompletableFuture.completedFuture(response)
        }
    }
}
