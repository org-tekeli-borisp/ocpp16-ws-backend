package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

class ChargePointRegistrySameSessionTest {

    @Test
    fun `register with same session and chargePointId keeps existing session connected`() {
        val registry = ChargePointRegistry()
        val oldAwaiter = ResponseAwaiter()
        val newAwaiter = ResponseAwaiter()

        registry.register("sess-same", "conn-1", "CP-SAME", oldAwaiter)
        val pending = oldAwaiter.pending("pending-1")

        registry.register("sess-same", "conn-2", "CP-SAME", newAwaiter)

        assertTrue(registry.isConnected("sess-same"))
        assertEquals(1, registry.connectionCount)
        assertFalse(pending.isCompletedExceptionally, "Old awaiter must not be rejected when re-registering the same session")
        assertEquals("conn-2", registry.getInfo("sess-same")!!.connectionId)
        assertSame(newAwaiter, registry.getResponseAwaiter("sess-same"))
    }
}
