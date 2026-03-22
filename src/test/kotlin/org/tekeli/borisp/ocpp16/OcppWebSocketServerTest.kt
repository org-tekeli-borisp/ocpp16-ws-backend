package org.tekeli.borisp.ocpp16

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@QuarkusTest
class OcppWebSocketServerTest {

    @Inject
    lateinit var vertx: Vertx

    @Test
    fun serverShouldStartAndExposeWebSocketEndpoint() {
        val messages = mutableListOf<String>()
        val lock = Any()
        val done = CountDownLatch(1)
        
        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")
        
        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                
                ws.handler { buffer: Buffer ->
                    synchronized(lock) {
                        messages.add(buffer.toString())
                        if (messages.size >= 1) {
                            done.countDown()
                            ws.close()
                        }
                    }
                }
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }
        
        val received = done.await(5, TimeUnit.SECONDS)
        assertTrue(received, "Should receive welcome message")
        synchronized(lock) {
            assertTrue(messages.isNotEmpty(), "Should receive welcome message")
            assertTrue(messages[0].contains("SupportedActions"))
        }
    }
    
    @Test
    fun shouldAcceptConnectionOnOcppEndpoint() {
        val latch = CountDownLatch(1)
        
        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")
        
        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ar.result().close()
                latch.countDown()
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }
        
        val connected = latch.await(5, TimeUnit.SECONDS)
        assertTrue(connected, "Connection should be accepted on /ocpp endpoint")
    }
    
    @Test
    fun shouldEchoMessageBack() {
        val latch = CountDownLatch(1)
        val messages = mutableListOf<String>()
        
        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")
        
        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                
                ws.handler { buffer: Buffer ->
                    messages.add(buffer.toString())
                    if (messages.size >= 2) {
                        latch.countDown()
                    }
                }
                
                ws.writeTextMessage("Hello OCPP")
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }
        
        val received = latch.await(5, TimeUnit.SECONDS)
        assertTrue(received, "Should receive echo response")
        assertEquals(2, messages.size, "Should receive welcome + echo response")
        assertTrue(messages[1].contains("Echo"))
        assertTrue(messages[1].contains("Hello OCPP"))
    }
}
