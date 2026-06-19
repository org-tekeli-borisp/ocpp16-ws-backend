package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.lang.reflect.Proxy
import java.time.Instant

class OcppWebSocketServerInfrastructureTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    private fun createWsProxy(): io.quarkus.websockets.next.WebSocketConnection =
        Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                String::class.java -> "proxy"
                Boolean::class.java -> false
                Uni::class.java -> Uni.createFrom().voidItem()
                else -> null
            }
        } as io.quarkus.websockets.next.WebSocketConnection

    @Test
    fun `should throw IllegalStateException when activeConnection accessed without initialization`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            server.activeConnection
        }
        assertEquals("Connection not initialized", exception.message)
    }

    @Test
    fun `activeConnection returns set connection when initialized`() {
        val s = OcppWebSocketServer().also { it.connection = createWsProxy() }
        assertDoesNotThrow({ s.activeConnection })
        assertEquals("proxy", s.activeConnection.id())
    }

    @Test
    fun `sendText delegates to set connection`() {
        var called = false
        val proxy = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, m, _ ->
            if (m.name == "sendText") called = true
            Uni.createFrom().voidItem()
        } as io.quarkus.websockets.next.WebSocketConnection
        val s = OcppWebSocketServer().also { it.connection = proxy }
        s.sendText("test")
        assertTrue(called, "sendText must delegate to set connection")
    }

    @Test
    fun `sendText returns void Uni when connection is null`() {
        val s = OcppWebSocketServer()
        assertDoesNotThrow({ s.sendText("test") })
    }

    @Test
    fun `metricsService messagesReceived increments on CALL message`() {
        val metrics = MetricsService()
        val s = OcppWebSocketServer().apply { chargePointId = "CP1"; metricsService = metrics }
        val c = metrics.messagesReceived.count()
        s.onTextMessage("""[2,"m1","Heartbeat",{}]""")
        assertEquals(c + 1, metrics.messagesReceived.count())
    }

    @Test
    fun `metricsService messagesReceived increments on BootNotification CALL`() {
        val metrics = MetricsService()
        val s = OcppWebSocketServer().apply { chargePointId = "CP1"; metricsService = metrics }
        val c = metrics.messagesReceived.count()
        s.onTextMessage("""[2,"m2","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""")
        assertEquals(c + 1, metrics.messagesReceived.count())
    }

    @Test
    fun `persistenceService createSecurityLog called for SecurityEventNotification`() {
        var logCreated = false
        val ps = object : PersistenceService() {
            override fun createSecurityLog(cpId: String, type: String, ts: Instant, ti: String?)
                : org.tekeli.borisp.ocpp16.persistence.SecurityLog {
                logCreated = true
                throw RuntimeException("intercepted")
            }
        }
        val s = OcppWebSocketServer().apply { chargePointId = "CP1"; sessionId = "s1"; persistenceService = ps }
        try {
            s.onTextMessage("""[2,"p1","SecurityEventNotification",{"type":"Tampering","timestamp":"2024-01-01T00:00:00Z"}]""")
        } catch (_: RuntimeException) {}
        assertTrue(logCreated, "createSecurityLog must be called")
    }

    @Test
    fun `null metricsService does not cause NPE on CALL`() {
        val s = OcppWebSocketServer().apply { chargePointId = "CP1"; metricsService = null }
        val r = s.onTextMessage("""[2,"n1","Heartbeat",{}]""")
        assertTrue(r.startsWith("[3,"))
    }
}
