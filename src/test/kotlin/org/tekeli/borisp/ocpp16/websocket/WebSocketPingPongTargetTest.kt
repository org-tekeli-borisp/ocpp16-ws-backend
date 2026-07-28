package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.websockets.next.CloseReason
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.lang.reflect.Proxy

class WebSocketPingPongTargetTest {

    private lateinit var target: WebSocketPingPongTarget
    private lateinit var mockConnection: io.quarkus.websockets.next.WebSocketConnection
    private lateinit var registry: ChargePointRegistry
    private lateinit var persistence: PersistenceService
    private lateinit var responseAwaiter: ResponseAwaiter
    private var rejectAwaiterCalled = false
    private var closeReason: CloseReason? = null
    private var rejectAwaiterMessage: String? = null
    private var offlineSessionId: String? = null

    @BeforeEach
    fun setUp() {
        registry = ChargePointRegistry()
        persistence = object : PersistenceService() {
            override fun setChargePointOffline(sessionId: String) {
                offlineSessionId = sessionId
            }
        }
        responseAwaiter = ResponseAwaiter()
        rejectAwaiterCalled = false
        offlineSessionId = null

        mockConnection = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "id" -> "test-conn-id"
                "sendPing" -> Uni.createFrom().voidItem()
                "close" -> {
                    val reason = args?.get(0) as? CloseReason
                    closeReason = reason
                    Uni.createFrom().voidItem()
                }
                else -> when (method.returnType) {
                    String::class.java -> "proxy"
                    Boolean::class.java -> false
                    Uni::class.java -> Uni.createFrom().voidItem()
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection

        target = WebSocketPingPongTarget(
            connection = { mockConnection },
            registry = registry,
            persistence = persistence,
            sessionId = "test-session",
            rejectAwaiterFn = { rejectAwaiterCalled = true; rejectAwaiterMessage = it }
        )
    }

    @Test
    fun `sendPing delegates to connection`() {
        val buffer = io.vertx.core.buffer.Buffer.buffer("test")

        val result = target.sendPing(buffer)

        assertNotNull(result)
        assertTrue(result is Uni<*>)
    }

    @Test
    fun `closeConnection closes with correct reason`() {
        val reason = "Connection lost"

        val result = target.closeConnection(reason)

        assertNotNull(result)
        assertNotNull(closeReason)
        assertEquals(1001, closeReason!!.code)
        assertTrue(closeReason!!.toString().contains("Connection lost"))
    }

    @Test
    fun `setChargePointOffline delegates to persistence`() {
        val sessionId = "offline-session"

        target.setChargePointOffline(sessionId)

        assertEquals(sessionId, offlineSessionId)
    }

    @Test
    fun `unregisterFromRegistry delegates to registry`() {
        val sessionId = "unreg-session"

        target.unregisterFromRegistry(sessionId)

        assertFalse(registry.isConnected(sessionId))
    }

    @Test
    fun `isConnected returns true when session is registered`() {
        registry.register("registered-session", "conn-1", null, responseAwaiter)

        val result = target.isConnected("registered-session")

        assertTrue(result)
    }

    @Test
    fun `isConnected returns false when session is not registered`() {
        val result = target.isConnected("unknown-session")

        assertFalse(result)
    }

    @Test
    fun `rejectAwaiter invokes callback with message`() {
        val message = "Connection closed"

        target.rejectAwaiter(message)

        assertTrue(rejectAwaiterCalled)
        assertEquals(message, rejectAwaiterMessage)
    }

    @Test
    fun `executeAsync runs runnable synchronously`() {
        var executed = false

        target.executeAsync { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `rejectAwaiter can reject responseAwaiter`() {
        val awaiter = ResponseAwaiter()
        val future = awaiter.pending("msg-1")

        val targetWithAwaiter = WebSocketPingPongTarget(
            connection = { mockConnection },
            registry = registry,
            persistence = persistence,
            sessionId = "test-session",
            rejectAwaiterFn = { awaiter.rejectAll(it) }
        )

        targetWithAwaiter.rejectAwaiter("Test rejection")

        assertTrue(future.isCompletedExceptionally)
    }
}
