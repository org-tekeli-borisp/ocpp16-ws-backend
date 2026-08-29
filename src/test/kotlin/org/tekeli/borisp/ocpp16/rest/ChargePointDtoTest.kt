package org.tekeli.borisp.ocpp16.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ConnectorStatusDto

class ChargePointDtoTest {

    private val connectors = listOf(
        ConnectorStatusDto(
            connectorId = 1,
            status = "Charging",
            errorCode = "NoError",
            info = null,
            timestamp = "2024-01-01T00:00:00Z"
        )
    )

    private fun dto() = ChargePointDto(
        id = 42L,
        chargePointId = "CP-001",
        vendor = "VendorX",
        model = "ModelY",
        firmwareVersion = "fw-1.0",
        status = "ONLINE",
        sessionId = "session-1",
        lastSeenAt = "2024-01-01T00:00:00Z",
        lastConnectedAt = "2023-12-31T23:00:00Z",
        createdAt = "2023-12-01T00:00:00Z",
        connectors = connectors
    )

    @Test
    fun `equal instances have equals and hashCode contract`() {
        assertEquals(dto(), dto())
        assertEquals(dto().hashCode(), dto().hashCode())
    }

    @Test
    fun `toString contains field values`() {
        assertTrue(dto().toString().contains("CP-001"))
    }

    @Test
    fun `copy without changes equals original`() {
        val original = dto()
        assertEquals(original, original.copy())
    }

    @Test
    fun `component functions return fields in order`() {
        val d = dto()
        assertEquals(42L, d.component1())
        assertEquals("CP-001", d.component2())
        assertEquals("VendorX", d.component3())
        assertEquals("ModelY", d.component4())
        assertEquals("fw-1.0", d.component5())
        assertEquals("ONLINE", d.component6())
        assertEquals("session-1", d.component7())
        assertEquals("2024-01-01T00:00:00Z", d.component8())
        assertEquals("2023-12-31T23:00:00Z", d.component9())
        assertEquals("2023-12-01T00:00:00Z", d.component10())
        assertEquals(connectors, d.component11())
    }

    @Test
    fun `instance with one different field is not equal`() {
        assertNotEquals(dto(), dto().copy(vendor = "OtherVendor"))
    }

    @Test
    fun `connectors defaults to empty list when omitted`() {
        val d = ChargePointDto(
            id = 7L,
            chargePointId = "CP-002",
            vendor = null,
            model = null,
            firmwareVersion = null,
            status = "OFFLINE",
            sessionId = "session-2",
            lastSeenAt = "2024-01-01T00:00:00Z",
            lastConnectedAt = "2024-01-01T00:00:00Z",
            createdAt = "2024-01-01T00:00:00Z"
        )
        assertTrue(d.connectors.isEmpty())
    }
}
