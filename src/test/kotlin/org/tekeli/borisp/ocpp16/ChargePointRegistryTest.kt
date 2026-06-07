package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.TextSender
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointConnection
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

class ChargePointRegistryTest {

    @Test
    fun `should register new session`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)

        assertTrue(registry.isConnected("session-1"))
        assertNotNull(registry.getInfo("session-1"))
        assertEquals(1, registry.connectionCount)
    }

    @Test
    fun `should unregister session`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.unregister("session-1")

        assertFalse(registry.isConnected("session-1"))
        assertNull(registry.getInfo("session-1"))
        assertEquals(0, registry.connectionCount)
    }

    @Test
    fun `should be idempotent when unregistering unknown session`() {
        val registry = ChargePointRegistry()

        assertDoesNotThrow {
            registry.unregister("unknown")
            registry.unregister("unknown")
        }
    }

    @Test
    fun `should update chargePointId after boot notification`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")

        val info = registry.getInfo("session-1")
        assertNotNull(info)
        assertEquals("CP-001", info!!.chargePointId)
        assertEquals("Tesla", info.vendor)
        assertEquals("Model3", info.model)
    }

    @Test
    fun `should throw when updating unknown session`() {
        val registry = ChargePointRegistry()

        assertThrows(IllegalStateException::class.java) {
            registry.updateChargePointInfo("unknown", "CP-001", "Tesla", "Model3")
        }
    }

    @Test
    fun `should find by chargePointId`() {
        val registry = ChargePointRegistry()
        val connection1 = mockChargePointConnection()
        val connection2 = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection1)
        registry.register("session-2", "conn-2", connection2)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")
        registry.updateChargePointInfo("session-2", "CP-002", "ABB", "Terra")

        val cp1 = registry.getByChargePointId("CP-001")
        val cp2 = registry.getByChargePointId("CP-002")

        assertNotNull(cp1)
        assertEquals("session-1", cp1!!.sessionId)
        assertNotNull(cp2)
        assertEquals("session-2", cp2!!.sessionId)
        assertNotSame(cp1, cp2)
    }

    @Test
    fun `should return null for unknown chargePointId`() {
        val registry = ChargePointRegistry()

        assertNull(registry.getByChargePointId("UNKNOWN"))
    }

    @Test
    fun `should handle multiple concurrent sessions`() {
        val registry = ChargePointRegistry()

        repeat(5) { i ->
            registry.register("session-$i", "conn-$i", mockChargePointConnection())
            registry.updateChargePointInfo("session-$i", "CP-$i", "Vendor$i", "Model$i")
        }

        assertEquals(5, registry.connectionCount)

        repeat(5) { i ->
            val info = registry.getByChargePointId("CP-$i")
            assertNotNull(info)
            assertEquals("Vendor$i", info!!.vendor)
        }
    }

    @Test
    fun `should remove from chargePointId index on unregister`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")
        registry.unregister("session-1")

        assertNull(registry.getByChargePointId("CP-001"))
        assertEquals(0, registry.connectionCount)
    }

    @Test
    fun `should return all connected session IDs`() {
        val registry = ChargePointRegistry()

        registry.register("session-1", "conn-1", mockChargePointConnection())
        registry.register("session-2", "conn-2", mockChargePointConnection())
        registry.register("session-3", "conn-3", mockChargePointConnection())

        val ids = registry.connectedSessionIds

        assertEquals(3, ids.size)
        assertTrue(ids.contains("session-1"))
        assertTrue(ids.contains("session-2"))
        assertTrue(ids.contains("session-3"))
    }

    @Test
    fun `should return all connected chargePoint IDs`() {
        val registry = ChargePointRegistry()

        registry.register("session-1", "conn-1", mockChargePointConnection())
        registry.register("session-2", "conn-2", mockChargePointConnection())
        registry.register("session-3", "conn-3", mockChargePointConnection())
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")
        registry.updateChargePointInfo("session-2", "CP-002", "ABB", "Terra")

        val ids = registry.connectedChargePointIds

        assertEquals(2, ids.size)
        assertTrue(ids.contains("CP-001"))
        assertTrue(ids.contains("CP-002"))
    }

    @Test
    fun `should send outbound call to specific chargePoint`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.setTestSender("session-1", connection)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")

        val future = registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))

        assertNotNull(future)
        assertFalse(future.isDone)

        val sentMessages = connection.sentMessages
        assertEquals(1, sentMessages.size)
        val call = OcppMessage.parse(sentMessages[0]) as OcppMessage.Call

        connection.simulateCallResult(call.messageId, mapOf("status" to "Accepted"))
        assertTrue(future.get().let { it is OcppMessage.CallResult })
    }

    @Test
    fun `should throw when sending to unknown chargePoint`() {
        val registry = ChargePointRegistry()

        assertThrows(IllegalStateException::class.java) {
            registry.sendCall("UNKNOWN", "Reset", mapOf("type" to "Hard"))
        }
    }

    @Test
    fun `should handle concurrent register and unregister`() {
        val registry = ChargePointRegistry()
        val threads = mutableListOf<Thread>()

        repeat(10) { i ->
            threads.add(Thread {
                val sessionId = "session-$i"
                registry.register(sessionId, "conn-$i", mockChargePointConnection())
                Thread.sleep(10)
                registry.unregister(sessionId)
            })
        }

        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }

        assertEquals(0, registry.connectionCount)
    }

    private fun mockChargePointConnection(): TestChargePointConnection = TestChargePointConnection()

    private class TestChargePointConnection(
        val sentMessages: MutableList<String> = mutableListOf()
    ) : ChargePointConnection {
        override val responseAwaiter = ResponseAwaiter()

        override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> {
            sentMessages.add(text)
            return io.smallrye.mutiny.Uni.createFrom().voidItem()
        }

        fun simulateCallResult(messageId: String, payload: Map<String, Any>) {
            responseAwaiter.resolve(
                messageId,
                OcppMessage.CallResult(messageId, payload)
            )
        }
    }
}
