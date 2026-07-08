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
}
