package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class OcppWebSocketServerPongTimeoutTest {

    private fun createWsProxy(
        onId: () -> String = { "test-session" },
        onCloseAction: () -> Unit = {},
        closeCodeRef: AtomicReference<Int?> = AtomicReference(null),
        closeReasonRef: AtomicReference<String?> = AtomicReference(null)
    ): io.quarkus.websockets.next.WebSocketConnection =
        Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "id" -> onId()
                "closeAndAwait" -> {
                    val cr = args?.get(0) as? io.quarkus.websockets.next.CloseReason
                    if (cr != null) {
                        closeCodeRef.set(cr.code)
                        closeReasonRef.set(cr.message)
                    }
                    onCloseAction()
                    null
                }
                "sendPing" -> Uni.createFrom().voidItem()
                "sendPong" -> Uni.createFrom().voidItem()
                else -> when (method.returnType) {
                    String::class.java -> "proxy"
                    Boolean::class.java -> false
                    Uni::class.java -> Uni.createFrom().voidItem()
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection

    @Test
    fun `pong timeout must close connection and set charge point offline when no pong received`() {
        var offlineCalled = false
        var offlineSessionId: String? = null
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
                offlineSessionId = sid
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)
        val closeCodeRef = AtomicReference<Int?>(null)
        val closeReasonRef = AtomicReference<String?>(null)

        val proxy = createWsProxy(
            onId = { "pong-timeout-session" },
            onCloseAction = { closed.set(true) },
            closeCodeRef = closeCodeRef,
            closeReasonRef = closeReasonRef
        )

        val server = OcppWebSocketServer().apply {
            currentConnection = proxy
            chargePointId = "PONG-CP"
            sessionId = "pong-timeout-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("pong-timeout-session", "pong-timeout-session", server, "PONG-CP")

        // Simulate: ping sent (isPinging = true), no pong received, timeout fires
        server.triggerPingAndPongTimeout()
        // Execute the pong timeout handler directly (simulates the scheduled task firing)
        server.executePongTimeout()

        assertTrue(offlineCalled, "setChargePointOffline must be called when pong timeout fires")
        assertEquals("pong-timeout-session", offlineSessionId, "Must call offline with correct session ID")
        assertTrue(closed.get(), "Connection should have been closed")
        assertEquals("pong-timeout-session", offlineSessionId, "Must call offline with correct session ID")
        assertEquals(1001, closeCodeRef.get(), "Must close with code 1001")
        assertEquals("Pong timeout", closeReasonRef.get(), "Must close with 'Pong timeout' reason")
    }

    @Test
    fun `pong received before timeout must NOT close connection`() {
        var offlineCalled = false
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)

        val proxy = createWsProxy(
            onId = { "pong-ok-session" },
            onCloseAction = { closed.set(true) }
        )

        val server = OcppWebSocketServer().apply {
            currentConnection = proxy
            chargePointId = "PONG-OK-CP"
            sessionId = "pong-ok-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("pong-ok-session", "pong-ok-session", server, "PONG-OK-CP")

        // Trigger ping, then simulate pong arrival before timeout
        server.triggerPingAndPongTimeout()
        server.onPongMessage(io.vertx.core.buffer.Buffer.buffer())

        // Now try to execute pong timeout - it should be a no-op since isPinging is false
        server.executePongTimeout()

        assertFalse(offlineCalled, "setChargePointOffline must NOT be called when pong is received in time")
        assertFalse(closed.get(), "Connection must NOT be closed when pong is received in time")
    }
}
