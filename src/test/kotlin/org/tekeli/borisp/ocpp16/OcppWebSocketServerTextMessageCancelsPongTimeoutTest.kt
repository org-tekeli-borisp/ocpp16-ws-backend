package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class OcppWebSocketServerTextMessageCancelsPongTimeoutTest {

    @Test
    fun `incoming text message must cancel pong timeout so connection is not closed`() {
        var offlineCalled = false
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)

        val server = OcppWebSocketServer().apply {
            chargePointId = "TEXT-MSG-CP"
            sessionId = "text-msg-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("text-msg-session", "text-msg-session", server, "TEXT-MSG-CP")

        val target = object : PingPongTarget {
            override fun sendPing(buffer: Buffer): Uni<Void> = Uni.createFrom().voidItem()
            override fun closeConnection(reason: String): Uni<Void> {
                closed.set(true)
                return Uni.createFrom().voidItem()
            }
            override fun setChargePointOffline(id: String) = ps.setChargePointOffline(id)
            override fun unregisterFromRegistry(id: String) = registry.unregister(id)
            override fun isConnected(id: String): Boolean = registry.isConnected(id)
            override fun rejectAwaiter(message: String) {}
            override fun executeAsync(runnable: Runnable) = runnable.run()
        }

        val scheduler = TestScheduler()
        val manager = PingPongManager(target, "text-msg-session", 1, 2, scheduler)
        manager.start()

        scheduler.executeNext() // Execute ping (isPinging = true, schedules pong timeout)

        // ChargePoint sends an OCPP message — should cancel the pong timeout
        manager.messageReceived()

        // Execute the rescheduled ping — pong timeout was cancelled
        scheduler.executeNext()

        assertFalse(offlineCalled, "setChargePointOffline must NOT be called when text message cancelled pong timeout")
        assertFalse(closed.get(), "Connection must NOT be closed when text message cancelled pong timeout")
    }

    @Test
    fun `incoming text message must cancel pong timeout for any valid OCPP action`() {
        var offlineCalled = false
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)

        val server = OcppWebSocketServer().apply {
            chargePointId = "ANY-ACTION-CP"
            sessionId = "any-action-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("any-action-session", "any-action-session", server, "ANY-ACTION-CP")

        val target = object : PingPongTarget {
            override fun sendPing(buffer: Buffer): Uni<Void> = Uni.createFrom().voidItem()
            override fun closeConnection(reason: String): Uni<Void> {
                closed.set(true)
                return Uni.createFrom().voidItem()
            }
            override fun setChargePointOffline(id: String) = ps.setChargePointOffline(id)
            override fun unregisterFromRegistry(id: String) = registry.unregister(id)
            override fun isConnected(id: String): Boolean = registry.isConnected(id)
            override fun rejectAwaiter(message: String) {}
            override fun executeAsync(runnable: Runnable) = runnable.run()
        }

        val scheduler = TestScheduler()
        val manager = PingPongManager(target, "any-action-session", 1, 2, scheduler)
        manager.start()

        val messages = listOf(
            """[2,"m1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""",
            """[2,"m2","StatusNotification",{"connectorId":1,"errorReason":"NoError","status":"Available","timestamp":"2024-01-01T00:00:00Z"}]""",
            """[2,"m3","MeterValues",{"connectorId":1,"transactionId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )

        for (msg in messages) {
            scheduler.executeNext() // Execute ping
            manager.messageReceived() // Simulate text message arrival
            scheduler.executeNext() // Execute rescheduled ping — pong timeout was cancelled
            assertFalse(offlineCalled, "Text message must cancel pong timeout for: $msg")
            assertFalse(closed.get(), "Connection must stay open for: $msg")
        }
    }
}
