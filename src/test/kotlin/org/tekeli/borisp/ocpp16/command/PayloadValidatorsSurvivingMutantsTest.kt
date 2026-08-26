package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.util.concurrent.CompletableFuture

class PayloadValidatorsSurvivingMutantsTest {

    @Test
    fun `safeMap returns empty map for null`() {
        assertTrue(PayloadValidators.safeMap(null).isEmpty())
    }

    @Test
    fun `safeMap returns empty map for non-map value`() {
        assertTrue(PayloadValidators.safeMap("not-a-map").isEmpty())
    }

    @Test
    fun `safeMap copies entries with exact keys and values`() {
        val result = PayloadValidators.safeMap(mapOf("a" to 1, "b" to "x"))

        assertEquals(2, result.size)
        assertEquals(1, result["a"])
        assertEquals("x", result["b"])
    }

    @Test
    fun `safeList returns empty list for null`() {
        assertTrue(PayloadValidators.safeList(null).isEmpty())
    }

    @Test
    fun `safeList returns empty list for non-list value`() {
        assertTrue(PayloadValidators.safeList("not-a-list").isEmpty())
    }

    @Test
    fun `safeList returns empty list for string value without throwing`() {
        assertTrue(PayloadValidators.safeList("a-string").isEmpty())
    }

    @Test
    fun `safeList keeps only string elements`() {
        assertEquals(listOf("a"), PayloadValidators.safeList(listOf(1, "a", null)))
    }

    @Test
    fun `safeList keeps all string elements`() {
        assertEquals(listOf("a", "b"), PayloadValidators.safeList(listOf("a", "b")))
    }

    @Test
    fun `ResetCommand rejects non-string type with BAD_REQUEST`() {
        val cmd = ResetCommand(StubGateway())

        val result = cmd.validate(mapOf<String, Any>("type" to 42))

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `ResetCommand accepts valid string type`() {
        val cmd = ResetCommand(StubGateway())

        assertNull(cmd.validate(mapOf<String, Any>("type" to "Hard")))
    }

    @Test
    fun `ChangeAvailabilityCommand rejects non-string type with BAD_REQUEST`() {
        val cmd = ChangeAvailabilityCommand(StubGateway())

        val result = cmd.validate(mapOf<String, Any>("connectorId" to 1, "type" to 42))

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `SendLocalListCommand rejects non-string updateType with BAD_REQUEST`() {
        val cmd = SendLocalListCommand(StubGateway())

        val result = cmd.validate(mapOf<String, Any>("listVersion" to 1, "updateType" to 42))

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `TriggerMessageCommand rejects non-string requestedMessage with BAD_REQUEST`() {
        val cmd = TriggerMessageCommand(StubGateway())

        val result = cmd.validate(mapOf<String, Any>("requestedMessage" to 42))

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    private class StubGateway : ChargePointGateway {
        private fun done(): CompletableFuture<OcppMessage> =
            CompletableFuture.completedFuture(OcppMessage.CallResult("id", mapOf()))

        override fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage> = done()
        override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage> = done()
        override fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage> = done()
        override fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage> = done()
        override fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage> = done()
        override fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage> = done()
        override fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage> = done()
        override fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage> = done()
        override fun sendGetConfiguration(chargePointId: String, keys: List<String>?): CompletableFuture<OcppMessage> = done()
        override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?, startTime: String?, stopTime: String?): CompletableFuture<OcppMessage> = done()
        override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = done()
        override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = done()
        override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = done()
        override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = done()
        override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = done()
        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = done()
        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = done()
        override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = done()
        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> = done()
        override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = done()
    }
}
