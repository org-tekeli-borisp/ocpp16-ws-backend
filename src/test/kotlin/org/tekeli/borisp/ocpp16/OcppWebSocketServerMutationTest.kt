package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.time.Instant

class OcppWebSocketServerMutationTest {

    private fun createWebSocketConnectionProxy(): io.quarkus.websockets.next.WebSocketConnection {
        return Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "id" -> "test-proxy-id"
                "sendText" -> {
                    val text = args?.get(0) as? String
                    if (text != null) {
                        Uni.createFrom().voidItem()
                    } else {
                        Uni.createFrom().voidItem()
                    }
                }
                "isClosed" -> false
                else -> when (method.returnType) {
                    String::class.java -> ""
                    Boolean::class.java -> false
                    Integer::class.java -> 0
                    Long::class.java -> 0L
                    Uni::class.java -> Uni.createFrom().voidItem()
                    java.util.Set::class.java -> emptySet<Any>()
                    java.util.Collection::class.java -> emptyList<Any>()
                    java.util.List::class.java -> emptyList<Any>()
                    java.util.Map::class.java -> emptyMap<Any, Any>()
                    Void::class.java -> null
                    else -> null
                }
            }
        } as io.quarkus.websockets.next.WebSocketConnection
    }

    // --- Mutant 1 & 2: Line 49 getActiveConnection ---
    // "removed call to getConnection" -> connection always null -> always throws
    // "removed conditional - replaced equality check with true" -> always throws
    // Kill by: setting connection via proxy and verifying activeConnection doesn't throw.

    @Test
    fun `activeConnection returns set connection when initialized`() {
        val server = OcppWebSocketServer()
        server.connection = createWebSocketConnectionProxy()

        assertDoesNotThrow({ server.activeConnection },
            "activeConnection must not throw when connection is set")
        assertEquals("test-proxy-id", server.activeConnection.id(),
            "activeConnection must return the set connection's id")
    }

    @Test
    fun `sendText delegates to set connection`() {
        var delegated = false
        val proxy = Proxy.newProxyInstance(
            io.quarkus.websockets.next.WebSocketConnection::class.java.classLoader,
            arrayOf(io.quarkus.websockets.next.WebSocketConnection::class.java)
        ) { _, method, args ->
            if (method.name == "sendText" && args?.get(0) == "hello world") {
                delegated = true
            }
            Uni.createFrom().voidItem()
        } as io.quarkus.websockets.next.WebSocketConnection

        val server = OcppWebSocketServer()
        server.connection = proxy
        server.sendText("hello world")

        assertTrue(delegated,
            "sendText must delegate to the set connection")
    }

    @Test
    fun `sendText returns void Uni when connection is null`() {
        val server = OcppWebSocketServer()
        val result = server.sendText("test")
        assertNotNull(result)
        assertTrue(result is Uni<*>)
    }

    // --- Mutant 3: Line 129 handleCall ---
    // "removed conditional - replaced equality check with false"
    // e.message ?: "Payload validation failed" -> always uses "Payload validation failed"
    // Kill by: asserting exact FormationViolationException message (non-null) is in response.

    @Test
    fun `formation violation preserves exact error message for empty vendor`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP1" }
        val response = server.onTextMessage(
            """[2,"e1","BootNotification",{"chargePointVendor":"","chargePointModel":"M"}]""")

        assertTrue(response.contains("chargePointVendor is required"),
            "Must contain exact error, not default: $response")
        assertFalse(response.contains("Payload validation failed"),
            "Must NOT contain default fallback: $response")
    }

    @Test
    fun `formation violation preserves exact error for null payload`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP1" }
        val response = server.onTextMessage("""[2,"e2","BootNotification",null]""")

        assertTrue(response.contains("Payload is null"),
            "Must contain 'Payload is null': $response")
        assertFalse(response.contains("Payload validation failed"),
            "Must NOT contain default fallback: $response")
    }

    @Test
    fun `formation violation preserves exact error for empty model`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP1" }
        val response = server.onTextMessage(
            """[2,"e3","BootNotification",{"chargePointVendor":"V","chargePointModel":""}]""")

        assertTrue(response.contains("chargePointModel is required"),
            "Must contain exact error: $response")
        assertFalse(response.contains("Payload validation failed"),
            "Must NOT contain default fallback: $response")
    }

    @Test
    fun `formation violation preserves exact error for vendor too long`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP1" }
        val longVendor = "A".repeat(21)
        val response = server.onTextMessage(
            """[2,"e4","BootNotification",{"chargePointVendor":"$longVendor","chargePointModel":"M"}]""")

        assertTrue(response.contains("chargePointVendor must not exceed 20 characters"),
            "Must contain exact length error: $response")
        assertFalse(response.contains("Payload validation failed"),
            "Must NOT contain default fallback: $response")
    }

    // --- Mutant 4: Line 65 handlers - removed call to getMetricsService ---
    // Kill by: setting real MetricsService, asserting messagesReceived counter increments.

    @Test
    fun `metricsService messagesReceived increments on Heartbeat CALL`() {
        val metrics = MetricsService()
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            metricsService = metrics
        }

        val counter = metrics.messagesReceived
        val countBefore = counter.count()
        server.onTextMessage("""[2,"m1","Heartbeat",{}]""")
        val countAfter = counter.count()

        assertEquals(countBefore + 1, countAfter,
            "messagesReceived counter must increment by 1 for CALL messages")
    }

    @Test
    fun `metricsService messagesReceived increments on BootNotification CALL`() {
        val metrics = MetricsService()
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            metricsService = metrics
        }

        val counter = metrics.messagesReceived
        val countBefore = counter.count()
        server.onTextMessage(
            """[2,"m2","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""")
        val countAfter = counter.count()

        assertEquals(countBefore + 1, countAfter,
            "Counter must increment - metricsService must be passed to handlers")
    }

    // --- Mutant 5: Line 72 handlers - removed call to getPersistenceService ---
    // Kill by: overriding createSecurityLog to intercept without calling super.

    @Test
    fun `persistenceService createSecurityLog called for SecurityEventNotification`() {
        var logCreated = false
        val persistence = object : PersistenceService() {
            override fun createSecurityLog(
                chargePointId: String, type: String, timestamp: Instant, techInfo: String?
            ): org.tekeli.borisp.ocpp16.persistence.SecurityLog {
                logCreated = true
                throw RuntimeException("Intercepted - persistenceService was called")
            }
        }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            sessionId = "sess1"
            persistenceService = persistence
        }

        try {
            server.onTextMessage(
                """[2,"p1","SecurityEventNotification",{"type":"Tampering","timestamp":"2024-01-01T00:00:00Z"}]""")
        } catch (_: RuntimeException) {
        }

        assertTrue(logCreated,
            "createSecurityLog must have been called - persistenceService passed to handler")
    }

    // --- Mutant 6: Line 102 onTextMessage catch OcppParseException ---
    // e.message ?: "Parse error" -> always uses "Parse error"
    // Kill by: asserting parse error response contains actual exception message.

    @Test
    fun `parse error preserves actual exception message`() {
        val server = OcppWebSocketServer()
        val response = server.onTextMessage("this is not valid json at all")

        assertTrue(response.contains("Failed to parse") || response.contains("parse"),
            "Must contain actual exception message, not default 'Parse error': $response")
        assertTrue(response.contains("ProtocolError"),
            "Must contain ProtocolError: $response")
    }

    @Test
    fun `parse error for empty string preserves actual message`() {
        val server = OcppWebSocketServer()
        val response = server.onTextMessage("")

        assertTrue(response.contains("Failed to parse") || response.contains("parse"),
            "Empty string parse error must contain actual message: $response")
        assertTrue(response.contains("ProtocolError"))
    }

    @Test
    fun `parse error for whitespace preserves actual message`() {
        val server = OcppWebSocketServer()
        val response = server.onTextMessage("   ")

        assertTrue(response.contains("Failed to parse") || response.contains("parse"),
            "Whitespace parse error must contain actual message: $response")
        assertTrue(response.contains("ProtocolError"))
    }

    // --- Mutant 7: Line 95 onTextMessage CALL branch ---
    // metricsService?.messagesReceived?.increment() -> unconditional increment (no null check)
    // Kill by: leaving metricsService null, verifying no NPE on CALL message.

    @Test
    fun `null metricsService safe on Heartbeat CALL`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            metricsService = null
        }
        val response = server.onTextMessage("""[2,"n1","Heartbeat",{}]""")

        assertTrue(response.startsWith("[3,"),
            "Must handle CALL with null metricsService without NPE: $response")
        assertTrue(response.contains("currentTime"))
    }

    @Test
    fun `null metricsService safe on BootNotification CALL`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            metricsService = null
        }
        val response = server.onTextMessage(
            """[2,"n2","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""")

        assertTrue(response.startsWith("[3,"),
            "Must handle BootNotification with null metricsService without NPE: $response")
    }

    @Test
    fun `null metricsService safe on multiple CALL types`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP1"
            metricsService = null
        }

        val r1 = server.onTextMessage("""[2,"n3","Heartbeat",{}]""")
        val r2 = server.onTextMessage(
            """[2,"n4","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""")
        val r3 = server.onTextMessage("""[2,"n5","Authorize",{"idTag":"TAG1"}]""")

        assertTrue(r1.startsWith("[3,"), "Heartbeat must succeed: $r1")
        assertTrue(r2.startsWith("[3,"), "BootNotification must succeed: $r2")
        assertTrue(r3.startsWith("[3,"), "Authorize must succeed: $r3")
    }
}
