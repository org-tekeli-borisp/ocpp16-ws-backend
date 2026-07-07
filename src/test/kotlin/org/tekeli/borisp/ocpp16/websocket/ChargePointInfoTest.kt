package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ChargePointInfoTest {

    @Test
    fun `constructor sets all fields`() {
        val info = ChargePointInfo(
            sessionId = "s1",
            connectionId = "conn-1",
            chargePointId = "CP-001",
            vendor = "Vendor",
            model = "Model"
        )

        assertEquals("s1", info.sessionId)
        assertEquals("conn-1", info.connectionId)
        assertEquals("CP-001", info.chargePointId)
        assertEquals("Vendor", info.vendor)
        assertEquals("Model", info.model)
    }

    @Test
    fun `constructor defaults chargePointId and vendor`() {
        val info = ChargePointInfo(
            sessionId = "s1",
            connectionId = "conn-1"
        )

        assertNull(info.chargePointId)
        assertNull(info.vendor)
        assertNull(info.model)
    }

    @Test
    fun `connectedAt is set to now`() {
        val before = Instant.now()
        val info = ChargePointInfo(sessionId = "s1", connectionId = "conn-1")
        val after = Instant.now()

        assertTrue(info.connectedAt.isAfter(before) || info.connectedAt.equals(before))
        assertTrue(info.connectedAt.isBefore(after) || info.connectedAt.equals(after))
    }

    @Test
    fun `copy creates new instance`() {
        val original = ChargePointInfo(sessionId = "s1", connectionId = "conn-1")
        val updated = original.copy(chargePointId = "CP-001", vendor = "Vendor")

        assertEquals("CP-001", updated.chargePointId)
        assertEquals("Vendor", updated.vendor)
        assertNull(original.chargePointId)
    }
}
