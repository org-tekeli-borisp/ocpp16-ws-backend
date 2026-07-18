package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class OcppWebSocketServerTextMessageCancelsPongTimeoutTest {

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
    fun `incoming text message must cancel pong timeout so connection is not closed`() {
        var offlineCalled = false
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)

        val proxy = createWsProxy(
            onId = { "text-msg-session" },
            onCloseAction = { closed.set(true) }
        )

        val server = OcppWebSocketServer().apply {
            currentConnection = proxy
            chargePointId = "TEXT-MSG-CP"
            sessionId = "text-msg-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("text-msg-session", "text-msg-session", server, "TEXT-MSG-CP")

        // Simulate: ping sent (isPinging = true), pong timeout scheduled
        server.triggerPingAndPongTimeout()

        // ChargePoint sends an OCPP message (e.g. Heartbeat) instead of a WebSocket Pong
        // This should cancel the pong timeout so connection is NOT closed
        server.onTextMessage("""[2,"m1","Heartbeat",{}]""")

        // Execute the pong timeout handler — it must be a no-op since text message cancelled it
        server.executePongTimeout()

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

        val proxy = createWsProxy(
            onId = { "any-action-session" },
            onCloseAction = { closed.set(true) }
        )

        val server = OcppWebSocketServer().apply {
            currentConnection = proxy
            chargePointId = "ANY-ACTION-CP"
            sessionId = "any-action-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("any-action-session", "any-action-session", server, "ANY-ACTION-CP")

        server.triggerPingAndPongTimeout()

        // Any valid OCPP message should cancel the pong timeout
        val messages = listOf(
            """[2,"m1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""",
            """[2,"m2","StatusNotification",{"connectorId":1,"errorReason":"NoError","status":"Available","timestamp":"2024-01-01T00:00:00Z"}]""",
            """[2,"m3","MeterValues",{"connectorId":1,"transactionId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )

        for (msg in messages) {
            server.onTextMessage(msg)
            server.executePongTimeout()
            assertFalse(offlineCalled, "Text message must cancel pong timeout for: $msg")
            assertFalse(closed.get(), "Connection must stay open for: $msg")

            // Re-trigger for next iteration
            server.triggerPingAndPongTimeout()
        }
    }
}
