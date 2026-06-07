package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.junit.jupiter.api.Test

class OcppCommandTest {

    private val validPayload = mapOf<String, Any>(
        "idTag" to "CARD123",
        "connectorId" to 1,
        "startTime" to "2024-01-01T00:00:00Z"
    )

    @Test
    fun `RemoteStartTransactionCommand has correct name`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())

        assertEquals("remote-start-transaction", cmd.name)
    }

    @Test
    fun `RemoteStartTransactionCommand rejects missing idTag`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "startTime" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `RemoteStartTransactionCommand rejects missing connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "startTime" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `RemoteStartTransactionCommand rejects null connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "startTime" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
    }

    @Test
    fun `RemoteStartTransactionCommand accepts valid payload`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())

        val result = cmd.validate(validPayload)

        assertNull(result)
    }

    @Test
    fun `RemoteStartTransactionCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStartTransactionCommand(service)

        val result = cmd.execute("CP-001", validPayload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    @Test
    fun `RemoteStartTransactionCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = RemoteStartTransactionCommand(service)

        val result = cmd.execute("CP-001", validPayload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
    }

    @Test
    fun `RemoteStartTransactionCommand execute uses correct idTag`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStartTransactionCommand(service)

        cmd.execute("CP-001", validPayload)

        assertEquals("CARD123", service.lastIdTag)
    }

    @Test
    fun `RemoteStartTransactionCommand execute uses correct connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStartTransactionCommand(service)

        cmd.execute("CP-001", validPayload)

        assertEquals(1, service.lastConnectorId)
    }

    // ---- RemoteStopTransactionCommand ----

    @Test
    fun `RemoteStopTransactionCommand has correct name`() {
        val cmd = RemoteStopTransactionCommand(TestOutboundService())

        assertEquals("remote-stop-transaction", cmd.name)
    }

    @Test
    fun `RemoteStopTransactionCommand rejects missing transactionId`() {
        val cmd = RemoteStopTransactionCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `RemoteStopTransactionCommand accepts valid payload`() {
        val cmd = RemoteStopTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>("transactionId" to 42)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `RemoteStopTransactionCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStopTransactionCommand(service)
        val payload = mapOf<String, Any>("transactionId" to 42)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    @Test
    fun `RemoteStopTransactionCommand execute uses correct transactionId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStopTransactionCommand(service)
        val payload = mapOf<String, Any>("transactionId" to 99)

        cmd.execute("CP-001", payload)

        assertEquals(99, service.lastTransactionId)
    }

    // ---- ResetCommand ----

    @Test
    fun `ResetCommand has correct name`() {
        val cmd = ResetCommand(TestOutboundService())

        assertEquals("reset", cmd.name)
    }

    @Test
    fun `ResetCommand rejects missing type`() {
        val cmd = ResetCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `ResetCommand rejects invalid type`() {
        val cmd = ResetCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "InvalidType")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `ResetCommand accepts Hard type`() {
        val cmd = ResetCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "Hard")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ResetCommand accepts Soft type`() {
        val cmd = ResetCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "Soft")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ResetCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ResetCommand(service)
        val payload = mapOf<String, Any>("type" to "Hard")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    @Test
    fun `ResetCommand execute uses correct type`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ResetCommand(service)
        val payload = mapOf<String, Any>("type" to "Soft")

        cmd.execute("CP-001", payload)

        assertEquals("Soft", service.lastResetType)
    }

    // ---- UnlockConnectorCommand ----

    @Test
    fun `UnlockConnectorCommand has correct name`() {
        val cmd = UnlockConnectorCommand(TestOutboundService())

        assertEquals("unlock-connector", cmd.name)
    }

    @Test
    fun `UnlockConnectorCommand rejects missing connectorId`() {
        val cmd = UnlockConnectorCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `UnlockConnectorCommand accepts valid payload`() {
        val cmd = UnlockConnectorCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 3)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `UnlockConnectorCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UnlockConnectorCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 3)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    @Test
    fun `UnlockConnectorCommand execute uses correct connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UnlockConnectorCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 5)

        cmd.execute("CP-001", payload)

        assertEquals(5, service.lastConnectorId)
    }

    // ---- Mock ----

    private class TestOutboundService(
        private val callResult: Boolean = true
    ) : ChargePointGateway {
        var lastIdTag: String? = null
        var lastConnectorId: Int? = null
        var lastTransactionId: Int? = null
        var lastResetType: String? = null

        override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastIdTag = idTag
            lastConnectorId = connectorId
            return java.util.concurrent.CompletableFuture.completedFuture(
                if (callResult) org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallResult("id", mapOf())
                else org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
            )
        }

        override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastTransactionId = transactionId
            return java.util.concurrent.CompletableFuture.completedFuture(
                if (callResult) org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallResult("id", mapOf())
                else org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
            )
        }

        override fun sendReset(chargePointId: String, type: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastResetType = type
            return java.util.concurrent.CompletableFuture.completedFuture(
                if (callResult) org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallResult("id", mapOf())
                else org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
            )
        }

        override fun sendUnlockConnector(chargePointId: String, connectorId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastConnectorId = connectorId
            return java.util.concurrent.CompletableFuture.completedFuture(
                if (callResult) org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallResult("id", mapOf())
                else org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)
            )
        }
    }
}
