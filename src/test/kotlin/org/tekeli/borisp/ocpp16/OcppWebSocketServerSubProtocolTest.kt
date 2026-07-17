package org.tekeli.borisp.ocpp16

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@QuarkusTest
class OcppWebSocketServerSubProtocolTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    private fun testPort(): Int = ocppUri.port

    @Test
    fun `should accept connection with correct subprotocol`() {
        val latch = CountDownLatch(1)
        val result = mutableListOf<Boolean>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/CP-SUBPROTOCOL-1")
            .setSubProtocols(listOf("ocpp1.6"))

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ar.result().close()
                result.add(true)
            } else {
                result.add(false)
            }
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should complete within timeout")
        assertTrue(result[0], "Connection with ocpp1.6 subprotocol should be accepted")
    }

    @Test
    fun `should reject connection without subprotocol`() {
        val latch = CountDownLatch(1)
        var wasClosed = false

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/CP-SUBPROTOCOL-2")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                ws.closeHandler { _ ->
                    wasClosed = true
                    latch.countDown()
                }
            } else {
                wasClosed = true
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should complete within timeout")
        assertTrue(wasClosed, "Connection without subprotocol should be closed by server per OCPP-J spec §3.1.2")
    }

    @Test
    fun `should reject connection with wrong subprotocol`() {
        val latch = CountDownLatch(1)
        val result = mutableListOf<Boolean>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/CP-SUBPROTOCOL-3")
            .setSubProtocols(listOf("ocpp2.0.1"))

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ar.result().close()
                result.add(false)
            } else {
                result.add(true)
            }
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should complete within timeout")
        assertTrue(result[0], "Connection with wrong subprotocol should be rejected")
    }

    @Test
    fun `should negotiate correct subprotocol`() {
        val latch = CountDownLatch(1)
        var negotiatedProtocol: String? = null

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/CP-SUBPROTOCOL-4")
            .setSubProtocols(listOf("ocpp1.6"))

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                negotiatedProtocol = ws.subProtocol()
                ws.close()
            }
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should complete within timeout")
        assertEquals("ocpp1.6", negotiatedProtocol, "Negotiated protocol should be ocpp1.6")
    }
}
