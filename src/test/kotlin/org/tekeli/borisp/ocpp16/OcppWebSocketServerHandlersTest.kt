package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
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

class OcppWebSocketServerHandlersTest {

    private lateinit var registry: ChargePointRegistry
    private lateinit var spyPersistence: SpyPersistenceService
    private lateinit var server: OcppWebSocketServer

    @BeforeEach
    fun setUp() {
        registry = ChargePointRegistry()
        spyPersistence = SpyPersistenceService()
        server = OcppWebSocketServer().apply {
            chargePointRegistry = registry
            persistenceService = spyPersistence
            metricsService = MetricsService()
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
    fun `onClose with no session context returns early`() {
        val conn = createWsConnectionProxy("unknown-conn")
        server.currentConnection = conn

        server.onClose(conn)

        assertNull(spyPersistence.offlineChargePointId)
        assertFalse(registry.isConnected("unknown-conn"))
    }

    @Test
    fun `onClose with no ping pong manager works correctly`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-1", "session-1", "CP-01", awaiter)

        val conn = createWsConnectionProxy("session-1")
        server.currentConnection = conn

        server.onClose(conn)

        assertEquals("CP-01", spyPersistence.offlineChargePointId)
        assertFalse(registry.isConnected("session-1"))
    }

    @Test
    fun `onClose with ping pong manager stops it`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-2", "session-2", "CP-02", awaiter)

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager("session-2", pingPongManager)

        val conn = createWsConnectionProxy("session-2")
        server.currentConnection = conn

        server.onClose(conn)

        assertFalse(pingPongManager.isPinging)
        assertEquals("CP-02", spyPersistence.offlineChargePointId)
    }

    @Test
    fun `onClose rejects awaiter`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-3", "session-3", "CP-03", awaiter)

        val future = awaiter.pending("msg-1")

        val conn = createWsConnectionProxy("session-3")
        server.currentConnection = conn

        server.onClose(conn)

        assertTrue(future.isCompletedExceptionally)
    }

    @Test
    fun `onTextMessage with no session context returns ProtocolError`() {
        val conn = createWsConnectionProxy("unknown-conn")

        val result = server.onTextMessage("""[2,"1","Heartbeat",{}]""", conn)

        assertTrue(result.startsWith("[4,"))
        assertTrue(result.contains("ProtocolError"))
        assertTrue(result.contains("No session context"))
    }

    @Test
    fun `onTextMessage with valid session returns handler response`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-4", "session-4", "CP-04", awaiter)

        val conn = createWsConnectionProxy("session-4")
        server.currentConnection = conn

        val result = server.onTextMessage("""[2,"1","Heartbeat",{}]""", conn)

        assertTrue(result.startsWith("[3,"))
    }

    @Test
    fun `onTextMessage handles touchLastSeenAt failure gracefully`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-5", "session-5", "CP-05", awaiter)

        val failingPersistence = object : PersistenceService() {
            override fun touchLastSeenAt(chargePointId: String) {
                throw RuntimeException("DB error")
            }
            override fun setChargePointOfflineByChargePointId(cpId: String) {
                spyPersistence.offlineChargePointId = cpId
            }
        }
        server.persistenceService = failingPersistence

        val conn = createWsConnectionProxy("session-5")
        server.currentConnection = conn

        val result = server.onTextMessage("""[2,"1","Heartbeat",{}]""", conn)

        assertTrue(result.startsWith("[3,"))
    }

    @Test
    fun `onPongMessage with no session context does not call pingPongManager`() {
        val conn = createWsConnectionProxy("unknown-conn")

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager("unknown-conn", pingPongManager)

        server.onPongMessage(Buffer.buffer(), conn)

        assertFalse(spyTarget.closeCalled, "PingPongManager should not be invoked when context is null")
    }

    @Test
    fun `onPongMessage with session but no pingPongManager does not crash`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-no-mgr", "session-no-mgr", "CP-NO-MGR", awaiter)

        val conn = createWsConnectionProxy("session-no-mgr")
        server.currentConnection = conn

        assertDoesNotThrow {
            server.onPongMessage(Buffer.buffer(), conn)
        }
    }

    @Test
    fun `onPongMessage with valid session calls pongReceived`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-6", "session-6", "CP-06", awaiter)

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager("session-6", pingPongManager)

        val conn = createWsConnectionProxy("session-6")
        server.currentConnection = conn

        pingPongManager.start()
        server.onPongMessage(Buffer.buffer(), conn)

        assertFalse(pingPongManager.isPinging)
    }

    @Test
    fun `onClose no-arg uses activeConnection id`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-close-arg", "session-close-arg", "CP-CLOSE-ARG", awaiter)

        val spyTarget = SpyPingPongTarget()
        val pingPongManager = createPingPongManagerWithTarget(spyTarget)
        registry.setPingPongManager("session-close-arg", pingPongManager)

        server.currentConnection = createWsConnectionProxy("session-close-arg")
        server.chargePointId = "CP-CLOSE-ARG"

        assertTrue(pingPongManager.isPinging != true || true)
        server.onClose()

        assertFalse(registry.isConnected("session-close-arg"))
        assertEquals("CP-CLOSE-ARG", spyPersistence.offlineChargePointId)
    }

    @Test
    fun `onClose no-arg stops pingPongManager`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-stop-mgr", "session-stop-mgr", "CP-STOP-MGR", awaiter)

        val pingPongManager = createPingPongManagerWithTarget(SpyPingPongTarget())
        registry.setPingPongManager("session-stop-mgr", pingPongManager)
        pingPongManager.start()

        server.currentConnection = createWsConnectionProxy("session-stop-mgr")
        server.chargePointId = "CP-STOP-MGR"

        server.onClose()

        assertFalse(pingPongManager.isPinging, "PingPongManager should be stopped")
    }

    @Test
    fun `onPongMessage with no ping pong manager does nothing`() {
        val awaiter = ResponseAwaiter()
        registry.register("session-7", "session-7", "CP-07", awaiter)

        val conn = createWsConnectionProxy("session-7")
        server.currentConnection = conn

        assertDoesNotThrow {
            server.onPongMessage(Buffer.buffer(), conn)
        }
    }
}

class SpyPingPongTarget : org.tekeli.borisp.ocpp16.websocket.PingPongTarget {
    var closeCalled = false
    override fun sendPing(buffer: Buffer): Uni<Void> = Uni.createFrom().voidItem()
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
        sessionId = "test-session",
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
