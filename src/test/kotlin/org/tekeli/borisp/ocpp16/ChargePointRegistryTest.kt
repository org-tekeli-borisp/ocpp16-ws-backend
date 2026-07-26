package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.TextSender
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.quarkus.websockets.next.OpenConnections
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.websocket.ChargePointConnection
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.SessionContext
import java.util.stream.Stream

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

    @Test
    fun `register properly stores session info and connection`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)

        val info = registry.getInfo("session-1")
        assertNotNull(info)
        assertEquals("session-1", info!!.sessionId)
        assertEquals("conn-1", info.connectionId)
        assertNull(info.chargePointId)
        assertNull(info.vendor)
        assertNull(info.model)

        val conn = registry.getConnection("session-1")
        assertNotNull(conn)
        assertSame(connection.responseAwaiter, conn!!.responseAwaiter)
    }

    @Test
    fun `unregister removes from all data structures`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")
        registry.unregister("session-1")

        assertNull(registry.getConnection("session-1"))
        assertNull(registry.getInfo("session-1"))
        assertNull(registry.getByChargePointId("CP-001"))
        assertFalse(registry.isConnected("session-1"))
        assertEquals(0, registry.connectionCount)
    }

    @Test
    fun `getByChargePointId returns correct info after update`() {
        val registry = ChargePointRegistry()
        registry.register("session-1", "conn-1", mockChargePointConnection())
        registry.updateChargePointInfo("session-1", "CP-001", "VendorA", "ModelX")

        val info = registry.getByChargePointId("CP-001")
        assertNotNull(info)
        assertEquals("CP-001", info!!.chargePointId)
        assertEquals("session-1", info.sessionId)
        assertEquals("conn-1", info.connectionId)
        assertEquals("VendorA", info.vendor)
        assertEquals("ModelX", info.model)
    }

    @Test
    fun `isConnected returns true for registered and false for unregistered`() {
        val registry = ChargePointRegistry()

        assertFalse(registry.isConnected("nonexistent"))
        registry.register("session-1", "conn-1", mockChargePointConnection())
        assertTrue(registry.isConnected("session-1"))
        assertFalse(registry.isConnected("other"))
        registry.unregister("session-1")
        assertFalse(registry.isConnected("session-1"))
    }

    @Test
    fun `connectedSessionIds and connectedChargePointIds are accurate after operations`() {
        val registry = ChargePointRegistry()

        registry.register("s1", "c1", mockChargePointConnection())
        registry.register("s2", "c2", mockChargePointConnection())
        registry.register("s3", "c3", mockChargePointConnection())
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")
        registry.updateChargePointInfo("s2", "CP-002", "V2", "M2")

        assertEquals(setOf("s1", "s2", "s3"), registry.connectedSessionIds)
        assertEquals(setOf("CP-001", "CP-002"), registry.connectedChargePointIds)

        registry.unregister("s2")

        assertEquals(setOf("s1", "s3"), registry.connectedSessionIds)
        assertEquals(setOf("CP-001"), registry.connectedChargePointIds)
        assertFalse(registry.connectedSessionIds.contains("s2"))
        assertFalse(registry.connectedChargePointIds.contains("CP-002"))
    }

    @Test
    fun `sendCall with unknown chargePointId throws IllegalStateException`() {
        val registry = ChargePointRegistry()

        val ex = assertThrows(IllegalStateException::class.java) {
            registry.sendCall("UNKNOWN", "Reset", mapOf("type" to "Hard"))
        }
        assertEquals("ChargePoint not connected: UNKNOWN", ex.message)
    }

    @Test
    fun `updateChargePointInfo updates all fields correctly`() {
        val registry = ChargePointRegistry()
        registry.register("session-1", "conn-1", mockChargePointConnection())

        registry.updateChargePointInfo("session-1", "CP-001", "Siemens", "VersiCharge")

        val info = registry.getInfo("session-1")
        assertNotNull(info)
        assertEquals("CP-001", info!!.chargePointId)
        assertEquals("Siemens", info.vendor)
        assertEquals("VersiCharge", info.model)
        assertEquals("session-1", info.sessionId)
        assertEquals("conn-1", info.connectionId)

        val cpInfo = registry.getByChargePointId("CP-001")
        assertNotNull(cpInfo)
        assertEquals("session-1", cpInfo!!.sessionId)
    }

    @Test
    fun `registering same session twice overwrites correctly`() {
        val registry = ChargePointRegistry()
        val connection1 = mockChargePointConnection()
        val connection2 = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection1)
        registry.updateChargePointInfo("session-1", "CP-001", "V1", "M1")
        registry.register("session-1", "conn-2", connection2)

        assertEquals(1, registry.connectionCount)

        val info = registry.getInfo("session-1")
        assertNotNull(info)
        assertEquals("session-1", info!!.sessionId)
        assertEquals("conn-2", info.connectionId)

        val conn = registry.getConnection("session-1")
        assertNotNull(conn)
        assertSame(connection2.responseAwaiter, conn!!.responseAwaiter)

        val cpInfo = registry.getByChargePointId("CP-001")
        assertNotNull(cpInfo)
        assertEquals("session-1", cpInfo!!.sessionId)
    }

    @Test
    fun `unregister only removes entries for the unregistered session`() {
        val registry = ChargePointRegistry()

        registry.register("s1", "c1", mockChargePointConnection())
        registry.register("s2", "c2", mockChargePointConnection())
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")
        registry.updateChargePointInfo("s2", "CP-002", "V2", "M2")

        registry.unregister("s1")

        assertNull(registry.getByChargePointId("CP-001"))

        val info2 = registry.getByChargePointId("CP-002")
        assertNotNull(info2)
        assertEquals("s2", info2!!.sessionId)
        assertEquals("CP-002", info2.chargePointId)
        assertEquals("V2", info2.vendor)
        assertEquals("M2", info2.model)

        assertTrue(registry.isConnected("s2"))
        assertNotNull(registry.getConnection("s2"))
        assertNotNull(registry.getInfo("s2"))
        assertEquals(1, registry.connectionCount)
        assertEquals(setOf("s2"), registry.connectedSessionIds)
        assertEquals(setOf("CP-002"), registry.connectedChargePointIds)
    }

    @Test
    fun `sendCall sends correct action and payload`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("session-1", "conn-1", connection)
        registry.setTestSender("session-1", connection)
        registry.updateChargePointInfo("session-1", "CP-001", "Tesla", "Model3")

        val future = registry.sendCall("CP-001", "StartTransaction", mapOf("connectorId" to 1, "idTag" to "TAG123"))

        assertNotNull(future)

        val sentMessages = connection.sentMessages
        assertEquals(1, sentMessages.size)
        val call = OcppMessage.parse(sentMessages[0]) as OcppMessage.Call

        assertEquals("StartTransaction", call.action)
        val payload = call.payload as Map<String, Any>
        assertEquals(1, payload["connectorId"])
        assertEquals("TAG123", payload["idTag"])
    }

    @Test
    fun `register increments metrics when metricsService is set`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply {
            injectedMeterRegistry = meterRegistry
            initGauges()
        }
        val registry = ChargePointRegistry()
        registry.metricsService = metricsService

        registry.register("s1", "c1", mockChargePointConnection())

        val gauge = meterRegistry.find("ocpp.charge.points.connected").gauge()
        assertNotNull(gauge)
        assertEquals(1.0, gauge!!.value())
    }

    @Test
    fun `unregister decrements metrics when metricsService is set`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply {
            injectedMeterRegistry = meterRegistry
            initGauges()
        }
        val registry = ChargePointRegistry()
        registry.metricsService = metricsService

        registry.register("s1", "c1", mockChargePointConnection())
        assertEquals(1.0, meterRegistry.find("ocpp.charge.points.connected").gauge()!!.value())

        registry.unregister("s1")
        assertEquals(0.0, meterRegistry.find("ocpp.charge.points.connected").gauge()!!.value())
    }

    @Test
    fun `sendCall increments messagesSent metric when metricsService is set`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply {
            injectedMeterRegistry = meterRegistry
        }
        val registry = ChargePointRegistry()
        registry.metricsService = metricsService

        val connection = mockChargePointConnection()
        registry.register("s1", "c1", connection)
        registry.setTestSender("s1", connection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        val beforeCount = meterRegistry.find("ocpp.messages.sent").counter()?.count() ?: 0.0
        registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        val afterCount = meterRegistry.find("ocpp.messages.sent").counter()?.count() ?: 0.0

        assertEquals(beforeCount + 1.0, afterCount)
    }

    @Test
    fun `unregister cleans testSenders so stale sender is not reused`() {
        val registry = ChargePointRegistry()
        val oldConnection = mockChargePointConnection()
        val newConnection = mockChargePointConnection()

        registry.register("s1", "c1", oldConnection)
        registry.setTestSender("s1", oldConnection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")
        registry.unregister("s1")

        // Re-register without setting a test sender
        registry.register("s1", "c2", newConnection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        // sendCall should throw because openConnections is not initialized
        // and testSenders should have been cleared (so it falls back to WsSender)
        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        }
    }

    @Test
    fun `sendCall throws IllegalStateException with correct sessionId when session context is missing`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("s1", "c1", connection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        // Remove the context from sessionContexts while keeping sessionInfo
        val sessionContextsField = registry::class.java.getDeclaredField("sessionContexts")
        sessionContextsField.isAccessible = true
        val sessionContexts = @Suppress("UNCHECKED_CAST") sessionContextsField.get(registry) as java.util.concurrent.ConcurrentHashMap<String, SessionContext>
        sessionContexts.remove("s1")

        val ex = assertThrows(IllegalStateException::class.java) {
            registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        }
        // Assert the error message contains the actual sessionId, not "null"
        assertTrue(ex.message!!.contains("s1"), "Error message should contain sessionId: ${ex.message}")
    }

    @Test
    fun `sendCall falls back to WsSender when testSender is not set`() {
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("s1", "c1", connection)
        // Intentionally NOT setting test sender
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        // Should throw because openConnections is not initialized
        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        }
    }

    @Test
    fun `sendCall works with initialized openConnections without testSender`() {
        val mockOpenConnections = object : OpenConnections {
            override fun stream() = Stream.empty<io.quarkus.websockets.next.WebSocketConnection>()
            override fun iterator() = mutableListOf<io.quarkus.websockets.next.WebSocketConnection>().iterator()
        }
        val registry = ChargePointRegistry()
        registry.openConnections = mockOpenConnections

        val connection = mockChargePointConnection()
        registry.register("s1", "c1", connection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")
        // Intentionally NOT setting test sender - forces WsSender path

        // With original code: sendCall returns CompletableFuture without throwing
        // With mutation (getOpenConnections EQUAL_ELSE): getter throws UninitializedPropertyAccessException
        assertDoesNotThrow {
            registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        }
    }

    @Test
    fun `should allow new connection to work after previous connection closes`() {
        // Simulates: connection 1 opens → closes (rejectAll) → connection 2 opens → sends command.
        // The bug was: ResponseAwaiter was shared across all connections, so rejectAll
        // on connection 1's close permanently poisoned the awaiter for connection 2.
        val registry = ChargePointRegistry()
        val connection1 = mockChargePointConnection()
        val connection2 = mockChargePointConnection()

        // Connection 1: register, close (with rejectAll)
        registry.register("s1", "c1", connection1)
        registry.setTestSender("s1", connection1)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")
        registry.unregister("s1")
        connection1.responseAwaiter.rejectAll("Connection 1 closed")

        // Connection 2: register (same chargePointId), should work normally
        registry.register("s2", "c2", connection2)
        registry.setTestSender("s2", connection2)
        registry.updateChargePointInfo("s2", "CP-001", "V2", "M2")

        // SendCall must work with connection 2's fresh awaiter
        val future = registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))
        assertFalse(future.isDone, "Future should NOT be immediately rejected")

        val sentMessages = connection2.sentMessages
        assertEquals(1, sentMessages.size)
        val call = OcppMessage.parse(sentMessages[0]) as OcppMessage.Call
        assertEquals("Reset", call.action)

        // Resolve the call normally
        connection2.simulateCallResult(call.messageId, mapOf("status" to "Accepted"))
        assertTrue((future.get() as OcppMessage.CallResult).payload!!.containsKey("status"))
    }

    @Test
    fun `should throw not connected when sendCall occurs after onClose sequence`() {
        // Simulates the correct onClose order: unregister first, then rejectAll.
        // After unregister, sendCall must throw "ChargePoint not connected"
        // and must NOT throw "ResponseAwaiter has been rejected".
        val registry = ChargePointRegistry()
        val connection = mockChargePointConnection()

        registry.register("s1", "c1", connection)
        registry.setTestSender("s1", connection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        // Correct onClose order: unregister first, then rejectAll
        registry.unregister("s1")
        connection.responseAwaiter.rejectAll("Connection closed")

        val ex = assertThrows(IllegalStateException::class.java) {
            registry.sendCall("CP-001", "TriggerMessage", mapOf("requestedMessage" to "Heartbeat"))
        }
        assertEquals("ChargePoint not connected: CP-001", ex.message)
    }

    @Test
    fun `sendCall captures outbound CALL and inbound response via MessageCaptureService`() {
        val capturedMessages = mutableListOf<OcppMessage>()
        val mockCaptureService = object : org.tekeli.borisp.ocpp16.protocol.MessageCaptureService() {
            override fun capture(chargePointId: String, direction: org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection, ocppMessage: OcppMessage) {
                super.capture(chargePointId, direction, ocppMessage)
                capturedMessages.add(ocppMessage)
            }
        }

        val registry = ChargePointRegistry()
        registry.messageCaptureService = mockCaptureService
        val connection = mockChargePointConnection()

        registry.register("s1", "c1", connection)
        registry.setTestSender("s1", connection)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        val future = registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard"))

        assertEquals(1, capturedMessages.size)
        val outboundCall = capturedMessages[0] as OcppMessage.Call
        assertEquals("Reset", outboundCall.action)

        connection.simulateCallResult(outboundCall.messageId, mapOf("status" to "Accepted"))
        future.get()

        assertEquals(2, capturedMessages.size)
        val inboundResponse = capturedMessages[1] as OcppMessage.CallResult
        assertEquals("Accepted", inboundResponse.payload?.get("status"))
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

    @Test
    fun `ChargePointRegistry sendCall with null metricsService`() {
        val registry = ChargePointRegistry()
        registry.metricsService = null
        val connection = mockChargePointConnection()
        registry.register("s-reg", "c-reg", connection)
        registry.setTestSender("s-reg", connection)
        registry.updateChargePointInfo("s-reg", "CP-REG", "V", "M")

        assertDoesNotThrow { registry.sendCall("CP-REG", "Heartbeat", emptyMap()) }
    }
}
