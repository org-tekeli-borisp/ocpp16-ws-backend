package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OcppMessageDirectionTest {

    @Test
    fun `fromValue INBOUND returns INBOUND`() {
        assertEquals(OcppMessageDirection.INBOUND, OcppMessageDirection.fromValue("INBOUND"))
    }

    @Test
    fun `fromValue OUTBOUND returns OUTBOUND`() {
        assertEquals(OcppMessageDirection.OUTBOUND, OcppMessageDirection.fromValue("OUTBOUND"))
    }

    @Test
    fun `fromValue unknown returns INBOUND default`() {
        assertEquals(OcppMessageDirection.INBOUND, OcppMessageDirection.fromValue("UNKNOWN"))
    }

    @Test
    fun `enum values have correct string representation`() {
        assertEquals("INBOUND", OcppMessageDirection.INBOUND.value)
        assertEquals("OUTBOUND", OcppMessageDirection.OUTBOUND.value)
    }
}
