package org.tekeli.borisp.ocpp16.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ConnectorStatusEntityTest {

    @Test
    fun `constructor with defaults`() {
        val cs = ConnectorStatus()

        assertEquals("", cs.chargePointId)
        assertEquals(0, cs.connectorId)
        assertEquals("", cs.status)
        assertEquals("", cs.errorCode)
        assertNull(cs.info)
    }

    @Test
    fun `constructor with all params`() {
        val now = Instant.now()
        val cs = ConnectorStatus(
            chargePointId = "CP-001",
            connectorId = 1,
            status = "Charging",
            errorCode = "NoError",
            info = "Connector OK",
            timestamp = now
        )

        assertEquals("CP-001", cs.chargePointId)
        assertEquals(1, cs.connectorId)
        assertEquals("Charging", cs.status)
        assertEquals("NoError", cs.errorCode)
        assertEquals("Connector OK", cs.info)
        assertEquals(now, cs.timestamp)
    }

    @Test
    fun `info is nullable`() {
        val cs = ConnectorStatus(
            chargePointId = "CP-001",
            connectorId = 1,
            status = "Available",
            errorCode = "NoError"
        )

        assertNull(cs.info)
    }

    @Test
    fun `id is null before persist`() {
        assertNull(ConnectorStatus().id)
    }

    @Test
    fun `setters update all fields`() {
        val cs = ConnectorStatus()
        val ts = Instant.parse("2024-04-04T04:04:04Z")

        cs.chargePointId = "CP-002"
        cs.connectorId = 3
        cs.status = "Faulted"
        cs.errorCode = "OtherError"
        cs.info = "Broken"
        cs.timestamp = ts

        assertEquals("CP-002", cs.chargePointId)
        assertEquals(3, cs.connectorId)
        assertEquals("Faulted", cs.status)
        assertEquals("OtherError", cs.errorCode)
        assertEquals("Broken", cs.info)
        assertEquals(ts, cs.timestamp)
    }
}
