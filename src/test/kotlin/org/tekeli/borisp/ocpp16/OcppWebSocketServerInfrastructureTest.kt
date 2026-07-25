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
        val s = OcppWebSocketServer().also { it.currentConnection = createWsProxy() }
        assertDoesNotThrow({ s.activeConnection })
        assertEquals("proxy", s.activeConnection.id())
    }

    @Test
    fun `sendText delegates to set connection`() {
        var delegated = false
        val proxy = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, args ->
            if (method.name == "sendText" && args?.get(0) == "test") {
                delegated = true
            }
            Uni.createFrom().voidItem()
        } as io.quarkus.websockets.next.WebSocketConnection
        val s = OcppWebSocketServer().also { it.currentConnection = proxy }
        s.sendText("test")
        assertTrue(delegated, "sendText must delegate exact argument to set connection")
    }

    @Test
    fun `sendText returns void Uni when connection is null`() {
        val s = OcppWebSocketServer()
        val result = s.sendText("test")
        assertNotNull(result)
        assertTrue(result is Uni<*>)
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
        assertTrue(r.contains("currentTime"))
    }

    @Test
    fun `null metricsService safe on multiple CALL types`() {
        val s = OcppWebSocketServer().apply { chargePointId = "CP1"; metricsService = null }

        val r1 = s.onTextMessage("""[2,"n3","Heartbeat",{}]""")
        val r2 = s.onTextMessage("""[2,"n4","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""")
        val r3 = s.onTextMessage("""[2,"n5","Authorize",{"idTag":"TAG1"}]""")

        assertTrue(r1.startsWith("[3,"), "Heartbeat must succeed")
        assertTrue(r2.startsWith("[3,"), "BootNotification must succeed")
        assertTrue(r3.startsWith("[3,"), "Authorize must succeed")
    }

    @Test
    fun `onClose must use activeConnection id not stale sessionId field`() {
        var offlineChargePointId: String? = null
        val ps = object : PersistenceService() {
            override fun setChargePointOfflineByChargePointId(cpId: String) {
                offlineChargePointId = cpId
            }
        }
        val registry = org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry()

        val proxy = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "id" -> "actual-connection-id"
                else -> when (method.returnType) {
                    String::class.java -> "proxy"
                    Boolean::class.java -> false
                    Uni::class.java -> Uni.createFrom().voidItem()
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection

        val s = OcppWebSocketServer().apply {
            currentConnection = proxy
            chargePointId = "CP1"
            sessionId = "stale-session-id"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("actual-connection-id", "actual-connection-id", s, "CP1")

        s.onClose()

        assertEquals("CP1", offlineChargePointId,
            "onClose must use chargePointId to set offline, not connection ID")
    }

    @Test
    fun `OcppWebSocketServer chargePointId returns exact value not empty`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-EXACT" }
        assertEquals("CP-EXACT", server.chargePointId)
        assertFalse(server.chargePointId!!.isEmpty())
    }

    @Test
    fun `OcppWebSocketServer onTextMessage increments messagesReceived`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-METRICS"
            sessionId = "sess-metrics"
            this.metricsService = metricsService
        }

        server.onTextMessage("""[2,"m1","Heartbeat",{}]""")

        val counter = meterRegistry.find("ocpp.messages.received").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `OcppWebSocketServer handleCall returns NotImplemented for unknown action`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-UNKNOWN" }
        val response = server.onTextMessage("""[2,"uk1","NonExistentAction",{}]""")
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("NonExistentAction"))
    }

    @Test
    fun `OcppWebSocketServer StartTransaction handler uses metricsService`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HANDLER-MS"
            sessionId = "sess-handler-ms"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"hm1","StartTransaction",{"connectorId":1,"idTag":"HM1","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val counter = meterRegistry.find("ocpp.messages.received").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `OcppWebSocketServer StopTransaction handler uses metricsService`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HANDLER-MS2"
            sessionId = "sess-handler-ms2"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"hm2","StopTransaction",{"transactionId":1,"meterStop":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val counter = meterRegistry.find("ocpp.transactions.stopped").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }
}
