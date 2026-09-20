package org.tekeli.borisp.ocpp16.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@QuarkusTest
@Timeout(30)
class UnexpectedResponseNoFrameTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    private val mapper = ObjectMapper()

    private fun testPort(): Int = ocppUri.port

    private fun connect(chargePointId: String, onText: (WebSocket, String) -> Unit): WebSocket {
        val connectLatch = CountDownLatch(1)
        val wsRef = AtomicReference<WebSocket>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/$chargePointId")
            .setSubProtocols(listOf("ocpp1.6"))

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                wsRef.set(ws)
                ws.textMessageHandler { message -> onText(ws, message) }
                connectLatch.countDown()
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        return wsRef.get()!!
    }

    @Test
    fun `should not send any frame for unexpected CALLRESULT`() {
        val cpId = "unexpected-result-${UUID.randomUUID()}"
        val frameLatch = CountDownLatch(1)
        val frames = mutableListOf<String>()

        val ws = connect(cpId) { _, message ->
            frames.add(message)
            frameLatch.countDown()
        }

        ws.writeTextMessage("""[3,"${UUID.randomUUID()}",{}]""")

        assertFalse(frameLatch.await(1, TimeUnit.SECONDS), "Unexpected CALLRESULT should be ignored without sending any frame")
        assertTrue(frames.isEmpty(), "No frame should be sent for unexpected CALLRESULT, got: $frames")

        ws.close()
    }

    @Test
    fun `should not send any frame for unexpected CALLERROR`() {
        val cpId = "unexpected-error-${UUID.randomUUID()}"
        val frameLatch = CountDownLatch(1)
        val frames = mutableListOf<String>()

        val ws = connect(cpId) { _, message ->
            frames.add(message)
            frameLatch.countDown()
        }

        ws.writeTextMessage("""[4,"${UUID.randomUUID()}","GenericError","err",{}]""")

        assertFalse(frameLatch.await(1, TimeUnit.SECONDS), "Unexpected CALLERROR should be ignored without sending any frame")
        assertTrue(frames.isEmpty(), "No frame should be sent for unexpected CALLERROR, got: $frames")

        ws.close()
    }

    @Test
    fun `should not send empty frame after charge point replies to outbound command`() {
        val cpId = "empty-frame-${UUID.randomUUID()}"
        val bootLatch = CountDownLatch(1)
        val commandLatch = CountDownLatch(1)
        val postReplyLatch = CountDownLatch(1)
        val messageIdRef = AtomicReference<String>()
        val booted = AtomicBoolean(false)

        val ws = connect(cpId) { ws, message ->
            if (!booted.get()) {
                if (message.startsWith("[3,")) {
                    booted.set(true)
                    bootLatch.countDown()
                }
            } else if (message.startsWith("[2,")) {
                val root: JsonNode = mapper.readTree(message)
                if (root.get(2).asText() == "ClearCache") {
                    val messageId = root.get(1).asText()
                    messageIdRef.set(messageId)
                    ws.writeTextMessage("""[3,"$messageId",{"status":"Accepted"}]""")
                    commandLatch.countDown()
                }
            } else {
                postReplyLatch.countDown()
            }
        }

        ws.writeTextMessage(
            """[2,"boot-1","BootNotification",{"chargePointVendor":"Tester","chargePointModel":"E2E","firmwareVersion":"1.0"}]"""
        )
        assertTrue(bootLatch.await(5, TimeUnit.SECONDS), "Should receive boot response")

        RestAssured.given()
            .contentType("application/json")
            .body("""{}""")
            .`when`().post("/api/chargepoints/$cpId/commands/clear-cache")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        assertTrue(commandLatch.await(5, TimeUnit.SECONDS), "Should receive ClearCache command")
        val messageId = messageIdRef.get()
        assertNotNull(messageId, "ClearCache command should carry a messageId")

        ws.writeTextMessage("""[3,"$messageId",{"status":"Accepted"}]""")

        assertFalse(postReplyLatch.await(1, TimeUnit.SECONDS), "No frame should be sent after the CALLRESULT reply")

        ws.close()
    }
}
