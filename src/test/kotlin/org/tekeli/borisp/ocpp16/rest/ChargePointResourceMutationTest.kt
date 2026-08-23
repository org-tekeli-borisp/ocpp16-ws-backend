package org.tekeli.borisp.ocpp16.rest

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.ConnectorStatusDto
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointInfo
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import java.time.Instant

class ChargePointResourceMutationTest {

    private lateinit var persistence: PersistenceService
    private lateinit var registry: ChargePointRegistry
    private lateinit var resource: ChargePointResource

    @BeforeEach
    fun setup() {
        persistence = mock(PersistenceService::class.java)
        registry = mock(ChargePointRegistry::class.java)
        resource = ChargePointResource()
        resource.persistenceService = persistence
        resource.chargePointRegistry = registry
    }

    private fun chargePoint(chargePointId: String, status: ChargePointStatus) = ChargePoint(
        chargePointId = chargePointId,
        vendor = "Vendor-$chargePointId",
        model = "Model-$chargePointId",
        firmwareVersion = "fw-$chargePointId",
        status = status,
        sessionId = "session-$chargePointId",
        lastSeenAt = Instant.parse("2024-06-02T00:00:00Z"),
        lastConnectedAt = Instant.parse("2024-06-01T00:00:00Z"),
        createdAt = Instant.parse("2024-05-01T00:00:00Z")
    )

    private fun connector(connectorId: Int, status: String) = ConnectorStatusDto(
        connectorId = connectorId,
        status = status,
        errorCode = "NO_ERROR",
        info = null,
        timestamp = "2024-06-02T00:00:00Z"
    )

    private fun connected(chargePointId: String) {
        `when`(registry.getByChargePointId(chargePointId)).thenReturn(
            ChargePointInfo(sessionId = "session-$chargePointId", connectionId = "conn-$chargePointId")
        )
    }

    @Test
    fun `getAll throws when persistenceService is not injected`() {
        val bare = ChargePointResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.getAll()
        }
    }

    @Test
    fun `getById throws when persistenceService is not injected`() {
        val bare = ChargePointResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.getById("CP-001")
        }
    }

    @Test
    fun `disconnect throws when chargePointRegistry is not injected`() {
        val bare = ChargePointResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.disconnect("CP-001")
        }
    }

    @Test
    fun `reconnectAll throws when chargePointRegistry is not injected`() {
        val bare = ChargePointResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.reconnectAll()
        }
    }

    @Test
    fun `getAll returns every charge point when no status filter is set`() {
        val online = chargePoint("CP-ON", ChargePointStatus.ONLINE)
        val offline = chargePoint("CP-OFF", ChargePointStatus.OFFLINE)
        `when`(persistence.findAllChargePoints()).thenReturn(listOf(online, offline))
        `when`(persistence.findConnectorStatusesByChargePointId("CP-ON")).thenReturn(emptyList())
        `when`(persistence.findConnectorStatusesByChargePointId("CP-OFF")).thenReturn(emptyList())
        connected("CP-ON")

        val result = resource.getAll()

        assertEquals(listOf("CP-ON", "CP-OFF"), result.map { it.chargePointId })
        assertEquals("ONLINE", result[0].status)
        assertEquals("OFFLINE", result[1].status)
    }

    @Test
    fun `getAll filters by status`() {
        val online = chargePoint("CP-ON", ChargePointStatus.ONLINE)
        `when`(persistence.findByStatus(ChargePointStatus.ONLINE)).thenReturn(listOf(online))
        `when`(persistence.findConnectorStatusesByChargePointId("CP-ON")).thenReturn(emptyList())
        connected("CP-ON")

        val result = resource.getAll("ONLINE")

        assertEquals(listOf("CP-ON"), result.map { it.chargePointId })
    }

    @Test
    fun `getAll throws BadRequestException listing valid statuses for an invalid status`() {
        val exception = assertThrows(BadRequestException::class.java) {
            resource.getAll("BOGUS")
        }

        assertEquals("Invalid status: BOGUS. Must be one of: ONLINE, OFFLINE", exception.message)
    }

    @Test
    fun `getById returns the mapped charge point`() {
        val cp = chargePoint("CP-001", ChargePointStatus.ONLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001"))
            .thenReturn(listOf(connector(1, "AVAILABLE"), connector(2, "CHARGING")))
        connected("CP-001")

        val dto = resource.getById("CP-001")

        assertNull(dto.id)
        assertEquals("CP-001", dto.chargePointId)
        assertEquals("Vendor-CP-001", dto.vendor)
        assertEquals("Model-CP-001", dto.model)
        assertEquals("fw-CP-001", dto.firmwareVersion)
        assertEquals("ONLINE", dto.status)
        assertEquals("session-CP-001", dto.sessionId)
        assertEquals("2024-06-02T00:00:00Z", dto.lastSeenAt)
        assertEquals("2024-06-01T00:00:00Z", dto.lastConnectedAt)
        assertEquals("2024-05-01T00:00:00Z", dto.createdAt)
        assertEquals(listOf(1, 2), dto.connectors.map { it.connectorId })
        assertEquals(listOf("AVAILABLE", "CHARGING"), dto.connectors.map { it.status })
    }

    @Test
    fun `getById throws NotFoundException for an unknown charge point`() {
        `when`(persistence.findChargePointById("missing")).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            resource.getById("missing")
        }
    }

    @Test
    fun `getById excludes connector 0 from the connector list`() {
        val cp = chargePoint("CP-001", ChargePointStatus.OFFLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001"))
            .thenReturn(listOf(connector(0, "AVAILABLE"), connector(1, "AVAILABLE"), connector(3, "FAULTED")))

        val dto = resource.getById("CP-001")

        assertEquals(listOf(1, 3), dto.connectors.map { it.connectorId })
    }

    @Test
    fun `getById returns empty connector list when no connector statuses are stored`() {
        val cp = chargePoint("CP-001", ChargePointStatus.OFFLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001")).thenReturn(emptyList())

        val dto = resource.getById("CP-001")

        assertTrue(dto.connectors.isEmpty())
    }

    @Test
    fun `effectiveStatus keeps ONLINE for a connected charge point`() {
        val cp = chargePoint("CP-001", ChargePointStatus.ONLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001")).thenReturn(emptyList())
        connected("CP-001")

        assertEquals("ONLINE", resource.getById("CP-001").status)
    }

    @Test
    fun `effectiveStatus reports OFFLINE for a stale ONLINE charge point`() {
        val cp = chargePoint("CP-001", ChargePointStatus.ONLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001")).thenReturn(emptyList())
        `when`(registry.getByChargePointId("CP-001")).thenReturn(null)

        assertEquals("OFFLINE", resource.getById("CP-001").status)
    }

    @Test
    fun `effectiveStatus keeps OFFLINE for a connected charge point`() {
        val cp = chargePoint("CP-001", ChargePointStatus.OFFLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001")).thenReturn(emptyList())
        connected("CP-001")

        assertEquals("OFFLINE", resource.getById("CP-001").status)
    }

    @Test
    fun `effectiveStatus keeps OFFLINE for a disconnected charge point`() {
        val cp = chargePoint("CP-001", ChargePointStatus.OFFLINE)
        `when`(persistence.findChargePointById("CP-001")).thenReturn(cp)
        `when`(persistence.findConnectorStatusesByChargePointId("CP-001")).thenReturn(emptyList())
        `when`(registry.getByChargePointId("CP-001")).thenReturn(null)

        assertEquals("OFFLINE", resource.getById("CP-001").status)
    }

    @Test
    fun `disconnect disconnects a connected charge point`() {
        connected("CP-001")

        val response = resource.disconnect("CP-001")

        assertTrue(response.disconnected)
        assertEquals("CP-001", response.chargePointId)
        verify(registry).disconnect("CP-001")
    }

    @Test
    fun `disconnect throws NotFoundException for a disconnected charge point`() {
        `when`(registry.getByChargePointId("CP-001")).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            resource.disconnect("CP-001")
        }
        verify(registry, never()).disconnect("CP-001")
    }

    @Test
    fun `reconnectAll returns the number of disconnected charge points`() {
        `when`(registry.disconnectAll()).thenReturn(3)

        val response = resource.reconnectAll()

        assertEquals(3, response.disconnectedCount)
    }

    @Test
    fun `DisconnectResponse exposes a falsy disconnected flag`() {
        val response = DisconnectResponse(disconnected = false, chargePointId = "CP-X")

        assertFalse(response.disconnected)
        assertEquals("CP-X", response.chargePointId)
    }
}
