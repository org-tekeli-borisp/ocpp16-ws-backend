package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
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

    private fun createWsConnectionProxy(id: String): io.quarkus.websockets.next.WebSocketConnection {
        var proxy: io.quarkus.websockets.next.WebSocketConnection? = null
        proxy = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { p, method, args ->
            when (method.name) {
                "id" -> id
                "hashCode" -> System.identityHashCode(p)
                "equals" -> args?.get(0) === p
                "toString" -> "WsProxy($id)"
                else -> when (method.returnType) {
                    String::class.java -> "proxy"
                    Boolean::class.java -> false
                    Uni::class.java -> Uni.createFrom().voidItem()
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection
        return proxy
    }

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

    @Test
    fun `responseAwaiter returns fallback when no session context`() {
        server.currentConnection = createWsConnectionProxy("no-context")

        val fallback = server.responseAwaiter

        assertNotNull(fallback)
        assertTrue(fallback is ResponseAwaiter)
    }

    @Test
    fun `chargePointId returns fallback when no session context`() {
        server.chargePointId = "fallback-cp"
        server.currentConnection = createWsConnectionProxy("no-context")

        val result = server.chargePointId

        assertEquals("fallback-cp", result)
    }

    @Test
    fun `sessionId returns fallback when no session context`() {
        server.sessionId = "fallback-session"
        server.currentConnection = createWsConnectionProxy("no-context")

        val result = server.sessionId

        assertEquals("fallback-session", result)
    }

}
