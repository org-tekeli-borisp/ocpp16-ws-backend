package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.lang.reflect.Proxy
import java.util.UUID

class OcppWebSocketServerMutationTest {

    private lateinit var registry: ChargePointRegistry
    private lateinit var server: OcppWebSocketServer
    private lateinit var spyPersistence: SpyPersistenceService

    @BeforeEach
    fun setUp() {
        registry = ChargePointRegistry()
        spyPersistence = SpyPersistenceService()
        server = OcppWebSocketServer().apply {
            chargePointRegistry = registry
            persistenceService = spyPersistence
        }
    }

    private fun createWsConnectionProxy(
        id: String?,
        calls: MutableList<Pair<String, Array<Any?>?>>? = null
    ): io.quarkus.websockets.next.WebSocketConnection {
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
                else -> {
                    calls?.add(method.name to (args ?: emptyArray()))
                    when (method.returnType) {
                        String::class.java -> "proxy"
                        Boolean::class.java -> false
                        Uni::class.java -> Uni.createFrom().voidItem()
                        else -> null
                    }
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection
        return proxy
    }

    @Test
    fun `responseAwaiter returns field fallback when currentConnection is null`() {
        val fallback = ResponseAwaiter()
        server.responseAwaiter = fallback

        assertSame(fallback, server.responseAwaiter)
    }

    @Test
    fun `getters return field fallbacks when connection id is null`() {
        val fallback = ResponseAwaiter()
        server.responseAwaiter = fallback
        server.chargePointId = "cp-null-id"
        server.sessionId = "session-null-id"
        server.currentConnection = createWsConnectionProxy(null)

        assertSame(fallback, server.responseAwaiter)
        assertEquals("cp-null-id", server.chargePointId)
        assertEquals("session-null-id", server.sessionId)
    }

    @Test
    fun `responseAwaiter returns session context awaiter when context exists`() {
        val ctxAwaiter = ResponseAwaiter()
        val fieldAwaiter = ResponseAwaiter()
        server.responseAwaiter = fieldAwaiter
        registry.register("s-1", "c-1", "cp-1", ctxAwaiter)
        server.currentConnection = createWsConnectionProxy("s-1")

        assertSame(ctxAwaiter, server.responseAwaiter)
    }

    @Test
    fun `chargePointId returns session context value when context exists`() {
        registry.register("s-2", "c-2", "cp-ctx-2", ResponseAwaiter())
        server.chargePointId = "cp-fallback-2"
        server.currentConnection = createWsConnectionProxy("s-2")

        assertEquals("cp-ctx-2", server.chargePointId)
    }

    @Test
    fun `sessionId returns session context value when context exists`() {
        registry.register("s-3", "c-3", "cp-3", ResponseAwaiter())
        server.sessionId = "fallback-3"
        server.currentConnection = createWsConnectionProxy("s-3")

        assertEquals("s-3", server.sessionId)
    }

    @Test
    fun `onClose no-arg stops ping pong manager looked up via session context`() {
        registry.register("s-4", "c-4", "cp-4", ResponseAwaiter())
        val manager = spy(createPingPongManagerWithTarget(SpyPingPongTarget()))
        registry.setPingPongManager("s-4", manager)
        server.currentConnection = createWsConnectionProxy("s-4")

        server.onClose()

        verify(manager).stop()
    }

    @Test
    fun `onClose no-arg with unknown connection does not throw`() {
        server.currentConnection = createWsConnectionProxy("unknown-mut")

        assertDoesNotThrow { server.onClose() }
    }

    @Test
    fun `onClose no-arg with context but no manager does not throw`() {
        registry.register("s-5", "c-5", "cp-5", ResponseAwaiter())
        server.currentConnection = createWsConnectionProxy("s-5")

        assertDoesNotThrow { server.onClose() }
    }

    @Test
    fun `onClose no-arg rejects awaiter with connection id in reason`() {
        val awaiter = ResponseAwaiter()
        registry.register("s-6", "c-6", "cp-6", awaiter)
        val future = awaiter.pending("m-6")
        server.currentConnection = createWsConnectionProxy("s-6")

        server.onClose()

        assertTrue(future.isCompletedExceptionally)
        val reason = future.exceptionNow()?.message
        assertTrue(reason?.contains("s-6") == true, "Rejection reason must contain connection id: $reason")
    }

    @Test
    fun `onTextMessage without session context returns UUID message id`() {
        val conn = createWsConnectionProxy("unknown-mut-2")

        val result = server.onTextMessage("""[2,"1","Heartbeat",{}]""", conn)

        val messageId = result.substringAfter("[4,\"").substringBefore("\"")
        assertDoesNotThrow({ UUID.fromString(messageId) }, "Message id must be a valid UUID, got: $result")
    }

    @Test
    fun `onTextMessage logs exception message when touchLastSeenAt fails`() {
        registry.register("s-7", "c-7", "cp-7", ResponseAwaiter())
        server.persistenceService = object : PersistenceService() {
            override fun touchLastSeenAt(chargePointId: String) {
                throw RuntimeException("db-touch-failure-marker")
            }
        }
        val conn = createWsConnectionProxy("s-7")
        server.currentConnection = conn

        val rootLogger = java.util.logging.Logger.getLogger("")
        val captured = mutableListOf<String>()
        val handler = object : java.util.logging.Handler() {
            override fun publish(record: java.util.logging.LogRecord) {
                record.message?.let { captured.add(it) }
            }
            override fun flush() {}
            override fun close() {}
        }
        rootLogger.addHandler(handler)
        try {
            val result = server.onTextMessage("""[2,"1","Heartbeat",{}]""", conn)
            assertTrue(result.startsWith("[3,"))
        } finally {
            rootLogger.removeHandler(handler)
        }

        assertTrue(
            captured.any { it.contains("db-touch-failure-marker") },
            "Log must contain exception message, got: $captured"
        )
    }

    @Test
    fun `onPongMessage calls pongReceived on manager from registry`() {
        registry.register("s-8", "c-8", "cp-8", ResponseAwaiter())
        val manager = spy(createPingPongManagerWithTarget(SpyPingPongTarget()))
        registry.setPingPongManager("s-8", manager)
        val conn = createWsConnectionProxy("s-8")

        server.onPongMessage(Buffer.buffer(), conn)

        verify(manager).pongReceived()
    }

    @Test
    fun `onPingMessage sends pong with same buffer`() {
        val calls = mutableListOf<Pair<String, Array<Any?>?>>()
        val conn = createWsConnectionProxy("s-9", calls)
        val buffer = Buffer.buffer("ping-data")

        val result = server.onPingMessage(buffer, conn)

        assertNotNull(result)
        val sendPongCalls = calls.filter { it.first == "sendPong" }
        assertEquals(1, sendPongCalls.size, "sendPong must be called exactly once: $calls")
        assertSame(buffer, sendPongCalls[0].second!![0])
    }
}
