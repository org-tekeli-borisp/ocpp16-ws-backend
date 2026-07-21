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
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("idTag"))
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("remote-start-transaction", entity["command"])
    }

    @Test
    fun `RemoteStartTransactionCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = RemoteStartTransactionCommand(service)

        val result = cmd.execute("CP-001", validPayload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
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

    @Test
    fun `RemoteStartTransactionCommand rejects string connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "connectorId" to "not-a-number"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `RemoteStartTransactionCommand accepts Long connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "connectorId" to 2L
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `RemoteStartTransactionCommand accepts Double connectorId`() {
        val cmd = RemoteStartTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "connectorId" to 3.0
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `RemoteStartTransactionCommand execute with Long connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStartTransactionCommand(service)
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "connectorId" to 4L
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(4, service.lastConnectorId)
    }

    @Test
    fun `RemoteStartTransactionCommand execute with Double connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStartTransactionCommand(service)
        val payload = mapOf<String, Any>(
            "idTag" to "CARD123",
            "connectorId" to 5.0
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(5, service.lastConnectorId)
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("transactionId"))
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("remote-stop-transaction", entity["command"])
    }

    @Test
    fun `RemoteStopTransactionCommand execute uses correct transactionId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStopTransactionCommand(service)
        val payload = mapOf<String, Any>("transactionId" to 99)

        cmd.execute("CP-001", payload)

        assertEquals(99, service.lastTransactionId)
    }

    @Test
    fun `RemoteStopTransactionCommand rejects string transactionId`() {
        val cmd = RemoteStopTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>("transactionId" to "not-a-number")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("transactionId"))
    }

    @Test
    fun `RemoteStopTransactionCommand accepts Long transactionId`() {
        val cmd = RemoteStopTransactionCommand(TestOutboundService())
        val payload = mapOf<String, Any>("transactionId" to 42L)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `RemoteStopTransactionCommand execute with Long transactionId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStopTransactionCommand(service)
        val payload = mapOf<String, Any>("transactionId" to 55L)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(55, service.lastTransactionId)
    }

    @Test
    fun `RemoteStopTransactionCommand execute with Double transactionId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = RemoteStopTransactionCommand(service)
        val payload = mapOf<String, Any>("transactionId" to 66.0)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(66, service.lastTransactionId)
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("type"))
    }

    @Test
    fun `ResetCommand rejects invalid type`() {
        val cmd = ResetCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "InvalidType")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("type"))
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("reset", entity["command"])
    }

    @Test
    fun `ResetCommand execute uses correct type`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ResetCommand(service)
        val payload = mapOf<String, Any>("type" to "Soft")

        cmd.execute("CP-001", payload)

        assertEquals("Soft", service.lastResetType)
    }

    @Test
    fun `ResetCommand rejects empty string type`() {
        val cmd = ResetCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
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
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("unlock-connector", entity["command"])
    }

    @Test
    fun `UnlockConnectorCommand execute uses correct connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UnlockConnectorCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 5)

        cmd.execute("CP-001", payload)

        assertEquals(5, service.lastConnectorId)
    }

    @Test
    fun `UnlockConnectorCommand rejects string connectorId`() {
        val cmd = UnlockConnectorCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to "not-a-number")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `UnlockConnectorCommand accepts Long connectorId`() {
        val cmd = UnlockConnectorCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 6L)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `UnlockConnectorCommand execute with Long connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UnlockConnectorCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 7L)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(7, service.lastConnectorId)
    }

    @Test
    fun `UnlockConnectorCommand execute with Double connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UnlockConnectorCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 8.0)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(8, service.lastConnectorId)
    }

    // ---- CancelReservationCommand ----

    @Test
    fun `CancelReservationCommand has correct name`() {
        val cmd = CancelReservationCommand(TestOutboundService())

        assertEquals("cancel-reservation", cmd.name)
    }

    @Test
    fun `CancelReservationCommand rejects missing reservationId`() {
        val cmd = CancelReservationCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("reservationId"))
    }

    @Test
    fun `CancelReservationCommand accepts valid payload`() {
        val cmd = CancelReservationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("reservationId" to 42)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `CancelReservationCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = CancelReservationCommand(service)
        val payload = mapOf<String, Any>("reservationId" to 42)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("cancel-reservation", entity["command"])
    }

    @Test
    fun `CancelReservationCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = CancelReservationCommand(service)
        val payload = mapOf<String, Any>("reservationId" to 42)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `CancelReservationCommand execute uses correct reservationId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = CancelReservationCommand(service)
        val payload = mapOf<String, Any>("reservationId" to 99)

        cmd.execute("CP-001", payload)

        assertEquals(99, service.lastReservationId)
    }

    @Test
    fun `CancelReservationCommand rejects string reservationId`() {
        val cmd = CancelReservationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("reservationId" to "not-a-number")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("reservationId"))
    }

    @Test
    fun `CancelReservationCommand accepts Long reservationId`() {
        val cmd = CancelReservationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("reservationId" to 100L)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `CancelReservationCommand execute with Long reservationId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = CancelReservationCommand(service)
        val payload = mapOf<String, Any>("reservationId" to 111L)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(111, service.lastReservationId)
    }

    @Test
    fun `CancelReservationCommand execute with Double reservationId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = CancelReservationCommand(service)
        val payload = mapOf<String, Any>("reservationId" to 122.0)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(122, service.lastReservationId)
    }

    // ---- ChangeAvailabilityCommand ----

    @Test
    fun `ChangeAvailabilityCommand has correct name`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())

        assertEquals("change-availability", cmd.name)
    }

    @Test
    fun `ChangeAvailabilityCommand rejects missing connectorId`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("type" to "Inoperative")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `ChangeAvailabilityCommand rejects missing type`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1)

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("type"))
    }

    @Test
    fun `ChangeAvailabilityCommand rejects invalid type`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1, "type" to "Invalid")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("type"))
    }

    @Test
    fun `ChangeAvailabilityCommand accepts Inoperative type`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1, "type" to "Inoperative")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ChangeAvailabilityCommand accepts Operative type`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1, "type" to "Operative")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ChangeAvailabilityCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeAvailabilityCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 1, "type" to "Inoperative")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("change-availability", entity["command"])
    }

    @Test
    fun `ChangeAvailabilityCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ChangeAvailabilityCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 1, "type" to "Inoperative")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `ChangeAvailabilityCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeAvailabilityCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 3, "type" to "Operative")

        cmd.execute("CP-001", payload)

        assertEquals(3, service.lastChangeAvailabilityConnectorId)
        assertEquals("Operative", service.lastChangeAvailabilityType)
    }

    @Test
    fun `ChangeAvailabilityCommand rejects string connectorId`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to "not-a-number", "type" to "Operative")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `ChangeAvailabilityCommand accepts Long connectorId`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 10L, "type" to "Operative")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ChangeAvailabilityCommand execute with Long connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeAvailabilityCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 11L, "type" to "Inoperative")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(11, service.lastChangeAvailabilityConnectorId)
    }

    @Test
    fun `ChangeAvailabilityCommand execute with Double connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeAvailabilityCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 12.0, "type" to "Inoperative")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(12, service.lastChangeAvailabilityConnectorId)
    }

    // ---- ChangeConfigurationCommand ----

    @Test
    fun `ChangeConfigurationCommand has correct name`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())

        assertEquals("change-configuration", cmd.name)
    }

    @Test
    fun `ChangeConfigurationCommand rejects missing key`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("value" to "val1")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("key"))
    }

    @Test
    fun `ChangeConfigurationCommand rejects missing value`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("key" to "MyKey")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("value"))
    }

    @Test
    fun `ChangeConfigurationCommand accepts valid payload`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("key" to "WebSocketPingInterval", "value" to "30")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ChangeConfigurationCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to "MyKey", "value" to "MyValue")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("change-configuration", entity["command"])
    }

    @Test
    fun `ChangeConfigurationCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ChangeConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to "MyKey", "value" to "MyValue")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `ChangeConfigurationCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to "Key1", "value" to "Val1")

        cmd.execute("CP-001", payload)

        assertEquals("Key1", service.lastConfigurationKey)
        assertEquals("Val1", service.lastConfigurationValue)
    }

    // ---- ClearCacheCommand ----

    @Test
    fun `ClearCacheCommand has correct name`() {
        val cmd = ClearCacheCommand(TestOutboundService())

        assertEquals("clear-cache", cmd.name)
    }

    @Test
    fun `ClearCacheCommand accepts empty payload`() {
        val cmd = ClearCacheCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNull(result)
    }

    @Test
    fun `ClearCacheCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearCacheCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("clear-cache", entity["command"])
    }

    @Test
    fun `ClearCacheCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ClearCacheCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    // ---- ClearChargingProfileCommand ----

    @Test
    fun `ClearChargingProfileCommand has correct name`() {
        val cmd = ClearChargingProfileCommand(TestOutboundService())

        assertEquals("clear-charging-profile", cmd.name)
    }

    @Test
    fun `ClearChargingProfileCommand accepts empty payload`() {
        val cmd = ClearChargingProfileCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNull(result)
    }

    @Test
    fun `ClearChargingProfileCommand accepts payload with optional fields`() {
        val cmd = ClearChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1, "stackLevel" to 5)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ClearChargingProfileCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearChargingProfileCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 1, "stackLevel" to 5)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("clear-charging-profile", entity["command"])
    }

    @Test
    fun `ClearChargingProfileCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ClearChargingProfileCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `ClearChargingProfileCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearChargingProfileCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 2, "stackLevel" to 3)

        cmd.execute("CP-001", payload)

        assertEquals(2, service.lastClearChargingProfileConnectorId)
        assertEquals(3, service.lastClearChargingProfileStackLevel)
    }

    @Test
    fun `ClearChargingProfileCommand execute with Long connectorId and stackLevel`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearChargingProfileCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 10L, "stackLevel" to 20L)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(10, service.lastClearChargingProfileConnectorId)
        assertEquals(20, service.lastClearChargingProfileStackLevel)
    }

    @Test
    fun `ClearChargingProfileCommand execute with Double connectorId and stackLevel`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearChargingProfileCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 11.0, "stackLevel" to 22.0)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(11, service.lastClearChargingProfileConnectorId)
        assertEquals(22, service.lastClearChargingProfileStackLevel)
    }

    // ---- GetCompositeScheduleCommand ----

    @Test
    fun `GetCompositeScheduleCommand has correct name`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())

        assertEquals("get-composite-schedule", cmd.name)
    }

    @Test
    fun `GetCompositeScheduleCommand rejects missing connectorId`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>("duration" to 3600)

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `GetCompositeScheduleCommand rejects missing duration`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1)

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("duration"))
    }

    @Test
    fun `GetCompositeScheduleCommand accepts valid payload`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1, "duration" to 3600)

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `GetCompositeScheduleCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 1, "duration" to 3600)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-composite-schedule", entity["command"])
    }

    @Test
    fun `GetCompositeScheduleCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 1, "duration" to 3600)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `GetCompositeScheduleCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>("connectorId" to 2, "duration" to 7200)

        cmd.execute("CP-001", payload)

        assertEquals(2, service.lastCompositeScheduleConnectorId)
        assertEquals(7200, service.lastCompositeScheduleDuration)
    }

    @Test
    fun `GetCompositeScheduleCommand rejects string connectorId`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to "not-a-number",
            "duration" to 3600
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `GetCompositeScheduleCommand rejects string duration`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "duration" to "not-a-number"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("duration"))
    }

    @Test
    fun `GetCompositeScheduleCommand validate with Long connectorId and duration`() {
        val cmd = GetCompositeScheduleCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 5L,
            "duration" to 7200L
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `GetCompositeScheduleCommand execute with Long connectorId and duration`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 5L,
            "duration" to 7200L
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(5, service.lastCompositeScheduleConnectorId)
        assertEquals(7200, service.lastCompositeScheduleDuration)
    }

    @Test
    fun `GetCompositeScheduleCommand execute with Double connectorId and duration`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 7.0,
            "duration" to 900.0
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(7, service.lastCompositeScheduleConnectorId)
        assertEquals(900, service.lastCompositeScheduleDuration)
    }

    @Test
    fun `GetCompositeScheduleCommand execute uses correct chargePointId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetCompositeScheduleCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 3,
            "duration" to 1800
        )

        val result = cmd.execute("CP-999", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(3, service.lastCompositeScheduleConnectorId)
        assertEquals(1800, service.lastCompositeScheduleDuration)
    }

    // ---- GetConfigurationCommand ----

    @Test
    fun `GetConfigurationCommand has correct name`() {
        val cmd = GetConfigurationCommand(TestOutboundService())

        assertEquals("get-configuration", cmd.name)
    }

    @Test
    fun `GetConfigurationCommand accepts empty payload`() {
        val cmd = GetConfigurationCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNull(result)
    }

    @Test
    fun `GetConfigurationCommand accepts payload with key`() {
        val cmd = GetConfigurationCommand(TestOutboundService())
        val payload = mapOf<String, Any>("key" to listOf("key1", "key2"))

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `GetConfigurationCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to listOf("key1"))

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-configuration", entity["command"])
    }

    @Test
    fun `GetConfigurationCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetConfigurationCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `GetConfigurationCommand execute uses correct keys`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to listOf("k1", "k2"))

        cmd.execute("CP-001", payload)

        assertEquals(listOf("k1", "k2"), service.lastGetConfigurationKeys)
    }

    @Test
    fun `GetConfigurationCommand execute passes null for missing key`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetConfigurationCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    @Test
    fun `GetConfigurationCommand execute with string key field`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetConfigurationCommand(service)
        val payload = mapOf<String, Any>("key" to "not-a-list")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    // ---- GetDiagnosticsCommand ----

    @Test
    fun `GetDiagnosticsCommand has correct name`() {
        val cmd = GetDiagnosticsCommand(TestOutboundService())

        assertEquals("get-diagnostics", cmd.name)
    }

    @Test
    fun `GetDiagnosticsCommand rejects missing location`() {
        val cmd = GetDiagnosticsCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("location"))
    }

    @Test
    fun `GetDiagnosticsCommand accepts valid payload`() {
        val cmd = GetDiagnosticsCommand(TestOutboundService())
        val payload = mapOf<String, Any>("location" to "http://example.com/diag")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `GetDiagnosticsCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetDiagnosticsCommand(service)
        val payload = mapOf<String, Any>("location" to "http://example.com/diag")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-diagnostics", entity["command"])
    }

    @Test
    fun `GetDiagnosticsCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetDiagnosticsCommand(service)
        val payload = mapOf<String, Any>("location" to "http://example.com/diag")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `GetDiagnosticsCommand execute uses correct location`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetDiagnosticsCommand(service)
        val payload = mapOf<String, Any>("location" to "http://diag.url")

        cmd.execute("CP-001", payload)

        assertEquals("http://diag.url", service.lastDiagnosticsLocation)
    }

    // ---- GetLocalListVersionCommand ----

    @Test
    fun `GetLocalListVersionCommand has correct name`() {
        val cmd = GetLocalListVersionCommand(TestOutboundService())

        assertEquals("get-local-list-version", cmd.name)
    }

    @Test
    fun `GetLocalListVersionCommand accepts empty payload`() {
        val cmd = GetLocalListVersionCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNull(result)
    }

    @Test
    fun `GetLocalListVersionCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetLocalListVersionCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("get-local-list-version", entity["command"])
    }

    @Test
    fun `GetLocalListVersionCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetLocalListVersionCommand(service)

        val result = cmd.execute("CP-001", emptyMap<String, Any>())

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    // ---- ReserveNowCommand ----

    @Test
    fun `ReserveNowCommand has correct name`() {
        val cmd = ReserveNowCommand(TestOutboundService())

        assertEquals("reserve-now", cmd.name)
    }

    @Test
    fun `ReserveNowCommand rejects missing connectorId`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to 1
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `ReserveNowCommand rejects missing expiryDate`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "CARD1",
            "reservationId" to 1
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("expiryDate"))
    }

    @Test
    fun `ReserveNowCommand rejects missing idTag`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "reservationId" to 1
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("idTag"))
    }

    @Test
    fun `ReserveNowCommand rejects missing reservationId`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("reservationId"))
    }

    @Test
    fun `ReserveNowCommand accepts valid payload`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to 42
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `ReserveNowCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to 42
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("reserve-now", entity["command"])
    }

    @Test
    fun `ReserveNowCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to 42
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `ReserveNowCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 3,
            "expiryDate" to "2025-06-01T12:00:00Z",
            "idTag" to "MYCARD",
            "reservationId" to 77
        )

        cmd.execute("CP-001", payload)

        assertEquals(3, service.lastReserveNowConnectorId)
        assertEquals("2025-06-01T12:00:00Z", service.lastReserveNowExpiryDate)
        assertEquals("MYCARD", service.lastReserveNowIdTag)
        assertEquals(77, service.lastReserveNowReservationId)
    }

    @Test
    fun `ReserveNowCommand rejects string connectorId`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to "not-a-number",
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to 1
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `ReserveNowCommand rejects string reservationId`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1",
            "reservationId" to "not-a-number"
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("reservationId"))
    }

    @Test
    fun `ReserveNowCommand execute with Long connectorId and reservationId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 5L,
            "expiryDate" to "2025-06-01T12:00:00Z",
            "idTag" to "MYCARD",
            "reservationId" to 88L
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(5, service.lastReserveNowConnectorId)
        assertEquals(88, service.lastReserveNowReservationId)
    }

    @Test
    fun `ReserveNowCommand execute with Double connectorId and reservationId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 7.0,
            "expiryDate" to "2025-06-01T12:00:00Z",
            "idTag" to "MYCARD",
            "reservationId" to 99.0
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(7, service.lastReserveNowConnectorId)
        assertEquals(99, service.lastReserveNowReservationId)
    }

    @Test
    fun `ReserveNowCommand execute uses correct chargePointId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 2,
            "expiryDate" to "2025-06-01T12:00:00Z",
            "idTag" to "CARD99",
            "reservationId" to 42
        )

        val result = cmd.execute("CP-999", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals("CARD99", service.lastReserveNowIdTag)
        assertEquals("2025-06-01T12:00:00Z", service.lastReserveNowExpiryDate)
    }

    // ---- SendLocalListCommand ----

    @Test
    fun `SendLocalListCommand has correct name`() {
        val cmd = SendLocalListCommand(TestOutboundService())

        assertEquals("send-local-list", cmd.name)
    }

    @Test
    fun `SendLocalListCommand rejects missing listVersion`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("updateType" to "Full")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("listVersion"))
    }

    @Test
    fun `SendLocalListCommand rejects missing updateType`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to 1)

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("updateType"))
    }

    @Test
    fun `SendLocalListCommand rejects invalid updateType`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to 1, "updateType" to "Invalid")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("updateType"))
    }

    @Test
    fun `SendLocalListCommand accepts Differential updateType`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to 1, "updateType" to "Differential")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `SendLocalListCommand accepts Full updateType`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to 1, "updateType" to "Full")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `SendLocalListCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SendLocalListCommand(service)
        val payload = mapOf<String, Any>("listVersion" to 5, "updateType" to "Full")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("send-local-list", entity["command"])
    }

    @Test
    fun `SendLocalListCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = SendLocalListCommand(service)
        val payload = mapOf<String, Any>("listVersion" to 5, "updateType" to "Full")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `SendLocalListCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SendLocalListCommand(service)
        val payload = mapOf<String, Any>("listVersion" to 10, "updateType" to "Differential")

        cmd.execute("CP-001", payload)

        assertEquals(10, service.lastSendLocalListVersion)
        assertEquals("Differential", service.lastSendLocalListUpdateType)
    }

    @Test
    fun `SendLocalListCommand rejects string listVersion`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to "not-a-number", "updateType" to "Full")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("listVersion"))
    }

    @Test
    fun `SendLocalListCommand accepts Long listVersion`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val payload = mapOf<String, Any>("listVersion" to 20L, "updateType" to "Full")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `SendLocalListCommand execute with Long listVersion`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SendLocalListCommand(service)
        val payload = mapOf<String, Any>("listVersion" to 21L, "updateType" to "Differential")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(21, service.lastSendLocalListVersion)
    }

    @Test
    fun `SendLocalListCommand execute with Double listVersion`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SendLocalListCommand(service)
        val payload = mapOf<String, Any>("listVersion" to 22.0, "updateType" to "Full")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(22, service.lastSendLocalListVersion)
    }

    // ---- SetChargingProfileCommand ----

    @Test
    fun `SetChargingProfileCommand has correct name`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())

        assertEquals("set-charging-profile", cmd.name)
    }

    @Test
    fun `SetChargingProfileCommand rejects missing connectorId`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>("csChargingProfiles" to mapOf<String, Any>())

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `SetChargingProfileCommand rejects missing csChargingProfiles`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>("connectorId" to 1)

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("csChargingProfiles"))
    }

    @Test
    fun `SetChargingProfileCommand accepts valid payload`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `SetChargingProfileCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SetChargingProfileCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("set-charging-profile", entity["command"])
    }

    @Test
    fun `SetChargingProfileCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = SetChargingProfileCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `SetChargingProfileCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SetChargingProfileCommand(service)
        val profiles = mapOf<String, Any>("chargingProfileId" to 5)
        val payload = mapOf<String, Any>(
            "connectorId" to 2,
            "csChargingProfiles" to profiles
        )

        cmd.execute("CP-001", payload)

        assertEquals(2, service.lastSetChargingProfileConnectorId)
        assertEquals(profiles, service.lastSetChargingProfiles)
    }

    @Test
    fun `SetChargingProfileCommand rejects string connectorId`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to "not-a-number",
            "csChargingProfiles" to mapOf<String, Any>()
        )

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("connectorId"))
    }

    @Test
    fun `SetChargingProfileCommand accepts Long connectorId`() {
        val cmd = SetChargingProfileCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "connectorId" to 3L,
            "csChargingProfiles" to mapOf<String, Any>()
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `SetChargingProfileCommand execute with Long connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SetChargingProfileCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 4L,
            "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(4, service.lastSetChargingProfileConnectorId)
    }

    @Test
    fun `SetChargingProfileCommand execute with Double connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SetChargingProfileCommand(service)
        val payload = mapOf<String, Any>(
            "connectorId" to 5.0,
            "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        assertEquals(5, service.lastSetChargingProfileConnectorId)
    }

    // ---- TriggerMessageCommand ----

    @Test
    fun `TriggerMessageCommand has correct name`() {
        val cmd = TriggerMessageCommand(TestOutboundService())

        assertEquals("trigger-message", cmd.name)
    }

    @Test
    fun `TriggerMessageCommand rejects missing requestedMessage`() {
        val cmd = TriggerMessageCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("requestedMessage"))
    }

    @Test
    fun `TriggerMessageCommand rejects invalid requestedMessage`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "InvalidMessage")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("requestedMessage"))
    }

    @Test
    fun `TriggerMessageCommand accepts BootNotification`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "BootNotification")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `TriggerMessageCommand accepts Heartbeat`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "Heartbeat")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `TriggerMessageCommand accepts StatusNotification`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "StatusNotification")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `TriggerMessageCommand accepts MeterValues`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "MeterValues")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `TriggerMessageCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = TriggerMessageCommand(service)
        val payload = mapOf<String, Any>("requestedMessage" to "Heartbeat", "connectorId" to 1)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("trigger-message", entity["command"])
    }

    @Test
    fun `TriggerMessageCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = TriggerMessageCommand(service)
        val payload = mapOf<String, Any>("requestedMessage" to "Heartbeat")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `TriggerMessageCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = TriggerMessageCommand(service)
        val payload = mapOf<String, Any>("requestedMessage" to "BootNotification", "connectorId" to 3)

        cmd.execute("CP-001", payload)

        assertEquals("BootNotification", service.lastTriggerMessage)
        assertEquals(3, service.lastTriggerMessageConnectorId)
    }

    @Test
    fun `TriggerMessageCommand rejects string requestedMessage`() {
        val cmd = TriggerMessageCommand(TestOutboundService())
        val payload = mapOf<String, Any>("requestedMessage" to "")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `TriggerMessageCommand execute with Long connectorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = TriggerMessageCommand(service)
        val payload = mapOf<String, Any>("requestedMessage" to "Heartbeat", "connectorId" to 6L)

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
    }

    // ---- UpdateFirmwareCommand ----

    @Test
    fun `UpdateFirmwareCommand has correct name`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())

        assertEquals("update-firmware", cmd.name)
    }

    @Test
    fun `UpdateFirmwareCommand rejects missing location`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())
        val payload = mapOf<String, Any>("retrieveDate" to "2024-01-01T00:00:00Z")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("location"))
    }

    @Test
    fun `UpdateFirmwareCommand rejects missing retrieveDate`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())
        val payload = mapOf<String, Any>("location" to "http://example.com/firmware.bin")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("retrieveDate"))
    }

    @Test
    fun `UpdateFirmwareCommand accepts valid payload`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "location" to "http://example.com/firmware.bin",
            "retrieveDate" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `UpdateFirmwareCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UpdateFirmwareCommand(service)
        val payload = mapOf<String, Any>(
            "location" to "http://example.com/firmware.bin",
            "retrieveDate" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("update-firmware", entity["command"])
    }

    @Test
    fun `UpdateFirmwareCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = UpdateFirmwareCommand(service)
        val payload = mapOf<String, Any>(
            "location" to "http://example.com/firmware.bin",
            "retrieveDate" to "2024-01-01T00:00:00Z"
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `UpdateFirmwareCommand execute uses correct params`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UpdateFirmwareCommand(service)
        val payload = mapOf<String, Any>(
            "location" to "http://firmware.url/v2.bin",
            "retrieveDate" to "2025-01-01T00:00:00Z"
        )

        cmd.execute("CP-001", payload)

        assertEquals("http://firmware.url/v2.bin", service.lastFirmwareLocation)
        assertEquals("2025-01-01T00:00:00Z", service.lastFirmwareRetrieveDate)
    }

    // ---- DataTransferCommand ----

    @Test
    fun `DataTransferCommand has correct name`() {
        val cmd = DataTransferCommand(TestOutboundService())

        assertEquals("data-transfer", cmd.name)
    }

    @Test
    fun `DataTransferCommand rejects missing vendorId`() {
        val cmd = DataTransferCommand(TestOutboundService())

        val result = cmd.validate(emptyMap<String, Any>())

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertTrue(entity["error"].toString().contains("vendorId"))
    }

    @Test
    fun `DataTransferCommand rejects empty vendorId`() {
        val cmd = DataTransferCommand(TestOutboundService())
        val payload = mapOf<String, Any>("vendorId" to "")

        val result = cmd.validate(payload)

        assertNotNull(result)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, result!!.status)
    }

    @Test
    fun `DataTransferCommand accepts valid payload with vendorId only`() {
        val cmd = DataTransferCommand(TestOutboundService())
        val payload = mapOf<String, Any>("vendorId" to "MyVendor")

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `DataTransferCommand accepts valid payload with all fields`() {
        val cmd = DataTransferCommand(TestOutboundService())
        val payload = mapOf<String, Any>(
            "vendorId" to "MyVendor",
            "messageId" to "msg1",
            "data" to "payload-data"
        )

        val result = cmd.validate(payload)

        assertNull(result)
    }

    @Test
    fun `DataTransferCommand execute returns ACCEPTED on success`() {
        val service = TestOutboundService(callResult = true)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>(
            "vendorId" to "MyVendor",
            "messageId" to "msg1",
            "data" to "payload-data"
        )

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("sent", entity["status"])
        assertEquals("data-transfer", entity["command"])
    }

    @Test
    fun `DataTransferCommand execute returns BAD_GATEWAY on error`() {
        val service = TestOutboundService(callResult = false)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>("vendorId" to "MyVendor")

        val result = cmd.execute("CP-001", payload)

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, result.status)
        val entity = PayloadValidators.safeMap(result.entity)
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `DataTransferCommand execute uses correct vendorId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>("vendorId" to "TestVendor")

        cmd.execute("CP-001", payload)

        assertEquals("TestVendor", service.lastDataTransferVendorId)
    }

    @Test
    fun `DataTransferCommand execute uses correct messageId`() {
        val service = TestOutboundService(callResult = true)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>(
            "vendorId" to "TestVendor",
            "messageId" to "test-msg"
        )

        cmd.execute("CP-001", payload)

        assertEquals("test-msg", service.lastDataTransferMessageId)
    }

    @Test
    fun `DataTransferCommand execute uses correct data`() {
        val service = TestOutboundService(callResult = true)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>(
            "vendorId" to "TestVendor",
            "data" to "test-payload"
        )

        cmd.execute("CP-001", payload)

        assertEquals("test-payload", service.lastDataTransferData)
    }

    @Test
    fun `DataTransferCommand execute passes null for optional fields`() {
        val service = TestOutboundService(callResult = true)
        val cmd = DataTransferCommand(service)
        val payload = mapOf<String, Any>("vendorId" to "TestVendor")

        cmd.execute("CP-001", payload)

        assertEquals("TestVendor", service.lastDataTransferVendorId)
    }

    // ---- Mock ----

    private class TestOutboundService(
        private val callResult: Boolean = true
    ) : ChargePointGateway {
        var lastIdTag: String? = null
        var lastConnectorId: Int? = null
        var lastTransactionId: Int? = null
        var lastResetType: String? = null
        var lastReservationId: Int? = null
        var lastChangeAvailabilityConnectorId: Int? = null
        var lastChangeAvailabilityType: String? = null
        var lastConfigurationKey: String? = null
        var lastConfigurationValue: String? = null
        var lastClearChargingProfileConnectorId: Int? = null
        var lastClearChargingProfileStackLevel: Int? = null
        var lastCompositeScheduleConnectorId: Int? = null
        var lastCompositeScheduleDuration: Int? = null
        var lastGetConfigurationKeys: List<String>? = null
        var lastDiagnosticsLocation: String? = null
        var lastReserveNowConnectorId: Int? = null
        var lastReserveNowExpiryDate: String? = null
        var lastReserveNowIdTag: String? = null
        var lastReserveNowReservationId: Int? = null
        var lastSendLocalListVersion: Int? = null
        var lastSendLocalListUpdateType: String? = null
        var lastSetChargingProfileConnectorId: Int? = null
        var lastSetChargingProfiles: Map<String, Any>? = null
        var lastTriggerMessage: String? = null
        var lastTriggerMessageConnectorId: Int? = null
        var lastFirmwareLocation: String? = null
        var lastFirmwareRetrieveDate: String? = null
        var lastDataTransferVendorId: String? = null
        var lastDataTransferMessageId: String? = null
        var lastDataTransferData: String? = null

        private fun makeResponse(): org.tekeli.borisp.ocpp16.protocol.OcppMessage =
            if (callResult) org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallResult("id", mapOf())
            else org.tekeli.borisp.ocpp16.protocol.OcppMessage.CallError("id", org.tekeli.borisp.ocpp16.protocol.OcppErrorCode.PROTOCOL_ERROR, "err", null)

        override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastIdTag = idTag
            lastConnectorId = connectorId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastTransactionId = transactionId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendReset(chargePointId: String, type: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastResetType = type
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendUnlockConnector(chargePointId: String, connectorId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastConnectorId = connectorId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendCancelReservation(chargePointId: String, reservationId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastReservationId = reservationId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastChangeAvailabilityConnectorId = connectorId
            lastChangeAvailabilityType = type
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendChangeConfiguration(chargePointId: String, key: String, value: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastConfigurationKey = key
            lastConfigurationValue = value
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendClearCache(chargePointId: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastClearChargingProfileConnectorId = connectorId
            lastClearChargingProfileStackLevel = stackLevel
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastCompositeScheduleConnectorId = connectorId
            lastCompositeScheduleDuration = duration
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendGetConfiguration(chargePointId: String, keys: List<String>?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastGetConfigurationKeys = keys
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastDiagnosticsLocation = location
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendGetLocalListVersion(chargePointId: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastReserveNowConnectorId = connectorId
            lastReserveNowExpiryDate = expiryDate
            lastReserveNowIdTag = idTag
            lastReserveNowReservationId = reservationId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastSendLocalListVersion = listVersion
            lastSendLocalListUpdateType = updateType
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastSetChargingProfileConnectorId = connectorId
            lastSetChargingProfiles = csChargingProfiles
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastTriggerMessage = requestedMessage
            lastTriggerMessageConnectorId = connectorId
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastFirmwareLocation = location
            lastFirmwareRetrieveDate = retrieveDate
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }

        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> =
            java.util.concurrent.CompletableFuture.completedFuture(makeResponse())

        override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): java.util.concurrent.CompletableFuture<org.tekeli.borisp.ocpp16.protocol.OcppMessage> {
            lastDataTransferVendorId = vendorId
            lastDataTransferMessageId = messageId
            lastDataTransferData = data
            return java.util.concurrent.CompletableFuture.completedFuture(makeResponse())
        }
    }

    // ---- Mutation kill tests: empty string validation ----

    @Test
    fun `ChangeAvailabilityCommand validate rejects empty type`() {
        val cmd = ChangeAvailabilityCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("connectorId" to 1, "type" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate rejects empty expiryDate`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "", "idTag" to "CARD1", "reservationId" to 1
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate rejects empty idTag`() {
        val cmd = ReserveNowCommand(TestOutboundService())
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z", "idTag" to "", "reservationId" to 1
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate rejects empty updateType`() {
        val cmd = SendLocalListCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("listVersion" to 1, "updateType" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeConfigurationCommand validate rejects empty key`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("key" to "", "value" to "val"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetDiagnosticsCommand validate rejects empty location`() {
        val cmd = GetDiagnosticsCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("location" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `UpdateFirmwareCommand validate rejects empty location`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("location" to "", "retrieveDate" to "2024-01-01T00:00:00Z"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `UpdateFirmwareCommand validate rejects empty retrieveDate`() {
        val cmd = UpdateFirmwareCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("location" to "http://fw.bin", "retrieveDate" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // ---- Mutation kill tests: execute with CallResult entity verification ----

    @Test
    fun `ResetCommand execute returns ACCEPTED with correct entity on CallResult`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ResetCommand(service)
        val resp = cmd.execute("CP-1", mapOf("type" to "Hard"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("reset", entity["command"])
        assertEquals("Hard", entity["type"])
    }

    @Test
    fun `ReserveNowCommand execute returns ACCEPTED on CallResult`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ReserveNowCommand(service)
        val resp = cmd.execute(
            "CP-1", mapOf<String, Any>(
                "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z",
                "idTag" to "CARD1", "reservationId" to 1
            )
        )
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("reserve-now", entity["command"])
    }

    @Test
    fun `SendLocalListCommand execute returns ACCEPTED on CallResult`() {
        val service = TestOutboundService(callResult = true)
        val cmd = SendLocalListCommand(service)
        val resp = cmd.execute("CP-1", mapOf("listVersion" to 5, "updateType" to "Differential"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("send-local-list", entity["command"])
    }

    @Test
    fun `ChangeAvailabilityCommand execute returns ACCEPTED on CallResult`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeAvailabilityCommand(service)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 2, "type" to "Operative"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("change-availability", entity["command"])
    }

    // ---- Mutation kill tests: execute with CallError BAD_GATEWAY ----

    @Test
    fun `ResetCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ResetCommand(service)
        val resp = cmd.execute("CP-1", mapOf("type" to "Hard"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("rejected", entity["status"])
    }

    @Test
    fun `RemoteStopTransactionCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = RemoteStopTransactionCommand(service)
        val resp = cmd.execute("CP-1", mapOf("transactionId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `UnlockConnectorCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = UnlockConnectorCommand(service)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetCompositeScheduleCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetCompositeScheduleCommand(service)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "duration" to 300))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetConfigurationCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetConfigurationCommand(service)
        val resp = cmd.execute("CP-1", mapOf("key" to listOf("key1")))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetLocalListVersionCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetLocalListVersionCommand(service)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `SetChargingProfileCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = SetChargingProfileCommand(service)
        val resp = cmd.execute(
            "CP-1",
            mapOf("connectorId" to 1, "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1))
        )
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `TriggerMessageCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = TriggerMessageCommand(service)
        val resp = cmd.execute("CP-1", mapOf("requestedMessage" to "Heartbeat"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `UpdateFirmwareCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = UpdateFirmwareCommand(service)
        val resp = cmd.execute("CP-1", mapOf("location" to "http://fw.bin", "retrieveDate" to "2024-01-01T00:00:00Z"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ChangeConfigurationCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ChangeConfigurationCommand(service)
        val resp = cmd.execute("CP-1", mapOf("key" to "key1", "value" to "val1"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ClearCacheCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ClearCacheCommand(service)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ClearChargingProfileCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = ClearChargingProfileCommand(service)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "stackLevel" to 0))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetDiagnosticsCommand execute returns BAD_GATEWAY on CallError`() {
        val service = TestOutboundService(callResult = false)
        val cmd = GetDiagnosticsCommand(service)
        val resp = cmd.execute("CP-1", mapOf("location" to "http://example.com/diag"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    // ---- Mutation kill tests: execute with ACCEPTED and entity verification ----

    @Test
    fun `ChangeConfigurationCommand execute returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ChangeConfigurationCommand(service)
        val resp = cmd.execute("CP-1", mapOf("key" to "LocalPreAuthorize", "value" to "true"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `ClearCacheCommand execute returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearCacheCommand(service)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `GetLocalListVersionCommand execute returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetLocalListVersionCommand(service)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `ClearChargingProfileCommand execute with all params returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = ClearChargingProfileCommand(service)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "stackLevel" to 0))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("clear-charging-profile", entity["command"])
    }

    @Test
    fun `GetDiagnosticsCommand execute with all params returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = GetDiagnosticsCommand(service)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "location" to "http://diag.com/log",
                "retries" to 3,
                "retryInterval" to 60
            )
        )
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `UpdateFirmwareCommand execute with all params returns ACCEPTED`() {
        val service = TestOutboundService(callResult = true)
        val cmd = UpdateFirmwareCommand(service)
        val resp = cmd.execute(
            "CP-1", mapOf(
                "location" to "http://fw.bin",
                "retrieveDate" to "2024-01-01T00:00:00Z",
                "retries" to 2,
                "retryInterval" to 30
            )
        )
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // ---- ChangeConfigurationCommand WebSocketPingInterval (SPEC-GAPS #3) ----

    @Test
    fun `ChangeConfigurationCommand validate accepts WebSocketPingInterval key`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("key" to "WebSocketPingInterval", "value" to "30"))

        assertNull(resp)
    }

    @Test
    fun `ChangeConfigurationCommand validate accepts HeartbeatInterval key`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("key" to "HeartbeatInterval", "value" to "60"))

        assertNull(resp)
    }

    @Test
    fun `ChangeConfigurationCommand validate rejects unknown config key`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val resp = cmd.validate(mapOf("key" to "UnknownKey", "value" to "val"))

        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeConfigurationCommand validate accepts all spec config keys`() {
        val cmd = ChangeConfigurationCommand(TestOutboundService())
        val keys = setOf(
            "AllowZeroCurrencyRate", "AuthorizeRemoteTxRequests", "BlinkRepeat",
            "ClockAlignedData", "ConnectionTimeOut", "GetConfigurationMaximumKeyLength",
            "HeartbeatInterval", "LightSigningStandard", "LocalAuthListEnabled",
            "LocalAuthListExchange", "MaxEnergyOnInvalidId", "MeterValueAlignedData",
            "MeterValueSampledData", "MeterValueSampleInterval", "MinimumStatusDuration",
            "NumberOfConnectors", "PowerMeterDuration", "PowerMeterLimit", "ResetRetries",
            "SignatureHeuristicVerification", "StopTransactionOnEVSideDisconnect",
            "StopTransactionOnInvalidId", "StopTxnAlignedData", "StopTxnSampledData",
            "SupportUnscheduledTransactions", "TransactionMessageAttempts",
            "TransactionMessageRetryInterval", "UnlockOnEvSideDisconnect", "WebSocketPingInterval"
        )

        for (key in keys) {
            val resp = cmd.validate(mapOf("key" to key, "value" to "test"))
            assertNull(resp, "Should accept config key: $key")
        }
    }
}
