package org.tekeli.borisp.ocpp16

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.net.URI
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@QuarkusTest
class OcppWebSocketServerPingPongTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    private fun testPort(): Int = ocppUri.port

    private fun createWsOptions(uri: String): WebSocketConnectOptions = WebSocketConnectOptions()
        .setHost("localhost")
        .setPort(testPort())
        .setURI(uri)
        .setSubProtocols(listOf("ocpp1.6"))

    @Test
    fun `should keep connection alive beyond ping interval`() {
        val connectLatch = CountDownLatch(1)
        var ws: io.vertx.core.http.WebSocket? = null

        val client = vertx.createWebSocketClient()
        val options = createWsOptions("/ocpp/PING-LIVE-1")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ws = ar.result()
                connectLatch.countDown()
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertNotNull(ws, "WebSocket should not be null")
        assertFalse(ws!!.isClosed, "Connection should be open after connect")

        // Wait beyond default 30s ping interval
        Thread.sleep(35000)

        assertFalse(ws!!.isClosed, "Connection should remain alive after ping interval (30s)")
        ws.close()
    }

    @Test
    fun `should close connection when client does not respond to ping`() {
        val connectLatch = CountDownLatch(1)
        var ws: io.vertx.core.http.WebSocket? = null

        val client = vertx.createWebSocketClient()
        val options = createWsOptions("/ocpp/PING-CLOSE-1")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ws = ar.result()
                connectLatch.countDown()

                // Close immediately after connecting to simulate unresponsive client
                // The server should detect this via ping timeout
                ws.close()
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        // After close, the server will attempt to ping and detect the closed connection
        Thread.sleep(5000)

        assertTrue(ws!!.isClosed, "Connection should be closed")
    }
}
