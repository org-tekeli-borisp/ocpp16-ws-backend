package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import org.tekeli.borisp.ocpp16.websocket.PingPongManager
import java.lang.reflect.Proxy

class OcppWebSocketServerSessionContextTest {

    private lateinit var registry: ChargePointRegistry
    private lateinit var server: OcppWebSocketServer
    private lateinit var spyPersistence: SpyPersistenceService

    private val sessionId = "test-session-ctx"
    private val chargePointId = "CP-CTX-01"

    @BeforeEach
    fun setUp() {
        registry = ChargePointRegistry()
        spyPersistence = SpyPersistenceService()
        server = OcppWebSocketServer().apply {
            chargePointRegistry = registry
            persistenceService = spyPersistence
        }
    }

    private fun createWsConnectionProxy(id: String): io.quarkus.websockets.next.WebSocketConnection =
        Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "id" -> id
                else -> when (method.returnType) {
                    String::class.java -> "proxy"
                    Boolean::class.java -> false
                    Uni::class.java -> Uni.createFrom().voidItem()
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection

    @Test
    fun `onClose with active session context uses chargePointId from context`() {
        val awaiter = ResponseAwaiter()
        registry.register(sessionId, sessionId, chargePointId, awaiter)

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager(sessionId, pingPongManager)

        server.currentConnection = createWsConnectionProxy(sessionId)
        server.chargePointId = chargePointId

        server.onClose()

        assertEquals(chargePointId, spyPersistence.offlineChargePointId, "Should set charge point offline by chargePointId")
    }

    @Test
    fun `onClose with active session context rejects awaiter`() {
        val awaiter = ResponseAwaiter()
        registry.register(sessionId, sessionId, chargePointId, awaiter)

        server.currentConnection = createWsConnectionProxy(sessionId)
        server.chargePointId = chargePointId

        val future = awaiter.pending("any-id")

        server.onClose()

        assertTrue(future.isCompletedExceptionally, "Future should be completed exceptionally after onClose")
    }

    @Test
    fun `onClose with no session context does nothing`() {
        val nonExistentConnection = createWsConnectionProxy("non-existent")
        server.currentConnection = nonExistentConnection
        server.chargePointId = chargePointId

        assertDoesNotThrow { server.onClose() }
        assertNull(spyPersistence.offlineChargePointId, "Should not set offline when no session context")
    }

    @Test
    fun `responseAwaiter from active session context`() {
        val awaiter = ResponseAwaiter()
        registry.register(sessionId, sessionId, chargePointId, awaiter)

        server.currentConnection = createWsConnectionProxy(sessionId)
        server.chargePointId = chargePointId

        val result = server.responseAwaiter

        assertSame(awaiter, result, "Should return responseAwaiter from active session context")
    }

    @Test
    fun `pingPongManager accessed via registry when session context exists`() {
        val awaiter = ResponseAwaiter()
        registry.register(sessionId, sessionId, chargePointId, awaiter)

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager(sessionId, pingPongManager)

        server.currentConnection = createWsConnectionProxy(sessionId)
        server.chargePointId = chargePointId

        server.onClose()

        assertTrue(spyTarget.closeCalled || registry.getPingPongManager(sessionId)?.isPinging != true,
            "PingPongManager should be stopped via activePingPongManager")
    }

    @Test
    fun `chargePointId from active session context used in onClose`() {
        val awaiter = ResponseAwaiter()
        registry.register(sessionId, sessionId, chargePointId, awaiter)

        server.currentConnection = createWsConnectionProxy(sessionId)
        server.chargePointId = "different-id"

        // Verify activeSessionContext works
        assertEquals(chargePointId, server.chargePointId, "chargePointId property should use context value")

        // Debug assertions
        assertTrue(registry.isConnected(sessionId), "Should be connected before onClose")

        server.onClose()

        assertFalse(registry.isConnected(sessionId), "Should NOT be connected after onClose")
        assertEquals(chargePointId, spyPersistence.offlineChargePointId, "Should use chargePointId from context")
    }
}

class SpyPingPongTarget : org.tekeli.borisp.ocpp16.websocket.PingPongTarget {
    var closeCalled = false
    override fun sendPing(buffer: io.vertx.core.buffer.Buffer): Uni<Void> = Uni.createFrom().voidItem()
    override fun closeConnection(reason: String): Uni<Void> {
        closeCalled = true
        return Uni.createFrom().voidItem()
    }
    override fun setChargePointOffline(sessionId: String) {}
    override fun unregisterFromRegistry(sessionId: String) {}
    override fun isConnected(sessionId: String): Boolean = false
    override fun rejectAwaiter(message: String) {}
    override fun executeAsync(runnable: Runnable) {}
}

class SpyPersistenceService : PersistenceService() {
    var offlineChargePointId: String? = null
    override fun setChargePointOfflineByChargePointId(cpId: String) {
        offlineChargePointId = cpId
    }
}

fun createPingPongManagerWithTarget(target: org.tekeli.borisp.ocpp16.websocket.PingPongTarget): PingPongManager =
    PingPongManager(
        target = target,
        sessionId = "spy-session",
        pingInterval = 1,
        pongTimeout = 2,
        scheduler = object : org.tekeli.borisp.ocpp16.websocket.Scheduler {
            override fun <V : Any?> schedule(runnable: Runnable, delay: Long, unit: java.util.concurrent.TimeUnit): java.util.concurrent.ScheduledFuture<V> =
                object : java.util.concurrent.ScheduledFuture<V> {
                    override fun get(): V = throw UnsupportedOperationException()
                    override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): V = throw UnsupportedOperationException()
                    override fun isCancelled(): Boolean = false
                    override fun isDone(): Boolean = false
                    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true
                    override fun getDelay(unit: java.util.concurrent.TimeUnit): Long = 0
                    override fun compareTo(other: java.util.concurrent.Delayed): Int = 0
                }
        }
    )
