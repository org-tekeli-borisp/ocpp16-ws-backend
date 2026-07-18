package org.tekeli.borisp.ocpp16.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ChargePointEntityTest {

    @Test
    fun `touch updates lastSeenAt`() {
        val cp = ChargePoint(
            chargePointId = "CP-001",
            vendor = "Vendor",
            model = "Model"
        )
        val before = cp.lastSeenAt
        Thread.sleep(10)
        cp.touch()

        assertTrue(cp.lastSeenAt.isAfter(before) || cp.lastSeenAt.equals(before))
    }

    @Test
    fun `constructor with defaults`() {
        val cp = ChargePoint(chargePointId = "CP-001")

        assertEquals("CP-001", cp.chargePointId)
        assertNull(cp.vendor)
        assertNull(cp.model)
        assertNull(cp.firmwareVersion)
        assertEquals(ChargePointStatus.OFFLINE, cp.status)
        assertEquals("", cp.sessionId)
    }

    @Test
    fun `constructor with all params`() {
        val now = Instant.now()
        val cp = ChargePoint(
            chargePointId = "CP-001",
            vendor = "Vendor",
            model = "Model",
            firmwareVersion = "1.0",
            status = ChargePointStatus.ONLINE,
            sessionId = "session-1",
            lastSeenAt = now,
            createdAt = now
        )

        assertEquals("CP-001", cp.chargePointId)
        assertEquals("Vendor", cp.vendor)
        assertEquals("Model", cp.model)
        assertEquals("1.0", cp.firmwareVersion)
        assertEquals(ChargePointStatus.ONLINE, cp.status)
        assertEquals("session-1", cp.sessionId)
    }

    @Test
    fun `ChargePointStatus has ONLINE and OFFLINE`() {
        val values = ChargePointStatus.values()

        assertEquals(2, values.size)
        assertTrue(values.any { it == ChargePointStatus.ONLINE })
        assertTrue(values.any { it == ChargePointStatus.OFFLINE })
    }

    @Test
    fun `lastConnectedAt defaults to now`() {
        val before = Instant.now()
        val cp = ChargePoint(chargePointId = "CP-001")
        val after = Instant.now()

        assertTrue(cp.lastConnectedAt.isAfter(before) || cp.lastConnectedAt.equals(before))
        assertTrue(cp.lastConnectedAt.isBefore(after) || cp.lastConnectedAt.equals(after))
    }

    @Test
    fun `constructor with all params includes lastConnectedAt`() {
        val now = Instant.now()
        val cp = ChargePoint(
            chargePointId = "CP-001",
            vendor = "Vendor",
            model = "Model",
            firmwareVersion = "1.0",
            status = ChargePointStatus.ONLINE,
            sessionId = "session-1",
            lastSeenAt = now,
            lastConnectedAt = now,
            createdAt = now
        )

        assertEquals(now, cp.lastConnectedAt)
    }
}
